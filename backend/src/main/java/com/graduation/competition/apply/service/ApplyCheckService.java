package com.graduation.competition.apply.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.ai.AiGateway;
import com.graduation.competition.apply.dto.ApplyCheckRequest;
import com.graduation.competition.apply.dto.ApplyCheckResponse;
import com.graduation.competition.apply.dto.ApplyFile;
import com.graduation.competition.apply.dto.CheckItemResult;
import com.graduation.competition.apply.dto.RequiredItem;
import com.graduation.competition.apply.entity.ApplicationCheck;
import com.graduation.competition.apply.repository.ApplicationCheckRepository;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.entity.CompetitionRule;
import com.graduation.competition.repository.CompetitionRepository;
import com.graduation.competition.repository.CompetitionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 依据有效规则提取材料清单并对照文件，不做报名表自动填写或复杂 OCR。
 */
@Service
@RequiredArgsConstructor
public class ApplyCheckService {

    private static final Pattern MATERIAL_PATTERN = Pattern.compile(
            "([一-龥A-Za-z0-9（）()、，,\\s]{2,30}?(?:报名表|作品说明|说明书|学生证|身份证|承诺书|推荐信|附件|成绩单|授权书))");

    private final CompetitionRepository competitionRepository;
    private final CompetitionRuleRepository ruleRepository;
    private final ApplicationCheckRepository checkRepository;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.provider:openai}")
    private String aiProvider;

    @Transactional
    public ApplyCheckResponse check(ApplyCheckRequest request) {
        Competition competition = competitionRepository.findByIdAndDeletedFalse(request.competitionId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "竞赛不存在"));
        if ("EXPIRED".equalsIgnoreCase(competition.getStatus())
                || competition.getContestEnd() != null
                && competition.getContestEnd().isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "竞赛已过期，不能使用当前规则检查材料");
        }
        List<CompetitionRule> rules = ruleRepository.findRules(
                competition.getId(), true, LocalDate.now());
        List<RequiredItem> required = extractRequired(rules);
        List<ApplyFile> files = request.files() == null ? List.of() : request.files();
        List<CheckItemResult> result = evaluate(required, files);

        try {
            ApplicationCheck entity = new ApplicationCheck();
            entity.setCompetitionId(competition.getId());
            entity.setFiles(objectMapper.writeValueAsString(files));
            entity.setRequiredList(objectMapper.writeValueAsString(required));
            entity.setResult(objectMapper.writeValueAsString(result));
            entity = checkRepository.saveAndFlush(entity);
            return new ApplyCheckResponse(entity.getId(), entity.getCompetitionId(),
                    required, result, entity.getCreatedAt());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "申报检查结果保存失败");
        }
    }

    @Transactional(readOnly = true)
    public ApplyCheckResponse get(Long id) {
        ApplicationCheck entity = checkRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "检查报告不存在"));
        try {
            return new ApplyCheckResponse(entity.getId(), entity.getCompetitionId(),
                    objectMapper.readValue(entity.getRequiredList(), new TypeReference<>() { }),
                    objectMapper.readValue(entity.getResult(), new TypeReference<>() { }),
                    entity.getCreatedAt());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "检查报告数据损坏");
        }
    }

    private List<RequiredItem> extractRequired(List<CompetitionRule> rules) {
        if (rules.isEmpty()) {
            return List.of();
        }
        Set<String> items = new LinkedHashSet<>();
        for (CompetitionRule rule : rules) {
            Matcher matcher = MATERIAL_PATTERN.matcher(rule.getContent());
            while (matcher.find()) {
                String raw = matcher.group(1)
                        .replaceAll("^[，,、。；;：:\\s]+", "")
                        .replaceAll("[，,、。；;：:\\s]+$", "")
                        .trim();
                Matcher exact = Pattern.compile(
                        "(报名表|作品说明|说明书|学生证|身份证|承诺书|推荐信|附件|成绩单|授权书)")
                        .matcher(raw);
                if (exact.find()) {
                    raw = exact.group(1);
                }
                if (raw.length() >= 2) {
                    items.add(normalizeItem(raw));
                }
            }
            if (rule.getTitle().contains("报名") || rule.getContent().contains("提交材料")) {
                items.add("报名表");
            }
        }
        if (!"extractive".equalsIgnoreCase(aiProvider)) {
            try {
                List<String> aiItems = parseAiItems(aiGateway.complete(
                        "从竞赛规则原文提取必须提交的材料名称。只输出 JSON 字符串数组；没有明确要求就输出 []，禁止补充。",
                        rules.stream().map(rule -> rule.getTitle() + "：" + rule.getContent())
                                .collect(java.util.stream.Collectors.joining("\n"))));
                for (String item : aiItems) {
                    if (supportedByRules(item, rules)) {
                        items.add(normalizeItem(item));
                    }
                }
            } catch (Exception ignored) {
                // 模型失败时保留规则原文正则提取结果。
            }
        }
        List<RequiredItem> result = new ArrayList<>();
        for (String item : items) {
            CompetitionRule source = rules.stream()
                    .filter(rule -> rule.getContent().contains(item)
                            || rule.getContent().contains(item.replace("复印件", ""))
                            || rule.getTitle().contains(item))
                    .findFirst().orElse(rules.get(0));
            result.add(new RequiredItem(item, true, source.getSourceUrl(), source.getSourceDate()));
        }
        return result;
    }

    private List<String> parseAiItems(String content) throws Exception {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("未返回 JSON 数组");
        }
        return objectMapper.readValue(content.substring(start, end + 1),
                new TypeReference<List<String>>() { });
    }

    private boolean supportedByRules(String item, List<CompetitionRule> rules) {
        String normalized = normalizeItem(item);
        return normalized.length() >= 2 && rules.stream()
                .anyMatch(rule -> rule.getContent().contains(normalized)
                        || rule.getTitle().contains(normalized));
    }

    private List<CheckItemResult> evaluate(List<RequiredItem> required, List<ApplyFile> files) {
        if (required.isEmpty()) {
            return List.of(new CheckItemResult("未提取到明确材料清单", "MISSING", null,
                    "有效规则中没有可识别的材料要求，系统不会猜测必需材料。", null, null));
        }
        List<CheckItemResult> results = new ArrayList<>();
        for (RequiredItem item : required) {
            ApplyFile best = files.stream()
                    .max(Comparator.comparingDouble(file -> nameScore(item.item(), file.name())))
                    .orElse(null);
            double score = best == null ? 0 : nameScore(item.item(), best.name());
            if (score >= 0.45) {
                results.add(new CheckItemResult(item.item(), "OK", best.name(), "",
                        item.source(), item.sourceDate()));
            } else if (score > 0) {
                results.add(new CheckItemResult(item.item(), "MISMATCH", best.name(),
                        "文件名与要求部分相关，请核对材料类型及内容是否一致。",
                        item.source(), item.sourceDate()));
            } else {
                results.add(new CheckItemResult(item.item(), "MISSING", null,
                        "缺少“" + item.item() + "”，请按官方规则补充。",
                        item.source(), item.sourceDate()));
            }
        }
        return results;
    }

    private double nameScore(String item, String fileName) {
        String required = normalizeItem(item);
        String name = fileName.toLowerCase(Locale.ROOT)
                .replaceAll("\\.(pdf|docx?|zip|rar|png|jpe?g)$", "");
        if (name.contains(required)) {
            return 1;
        }
        int matched = 0;
        for (int index = 0; index + 2 <= required.length(); index += 2) {
            if (name.contains(required.substring(index, index + 2))) {
                matched++;
            }
        }
        return (double) matched / Math.max(1, required.length() / 2);
    }

    private String normalizeItem(String item) {
        return item.replaceAll("[\\s，,、。；;：:（）()]", "")
                .replace("学生证复印件", "学生证")
                .replace("身份证复印件", "身份证");
    }
}

