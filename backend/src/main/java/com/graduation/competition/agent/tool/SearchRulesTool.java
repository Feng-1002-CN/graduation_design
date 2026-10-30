package com.graduation.competition.agent.tool;

import com.graduation.competition.agent.dto.EvidenceItem;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.entity.CompetitionRule;
import com.graduation.competition.rag.embedding.EmbeddingClient;
import com.graduation.competition.rag.vector.SearchHit;
import com.graduation.competition.rag.vector.SearchOptions;
import com.graduation.competition.rag.vector.VectorStoreService;
import com.graduation.competition.repository.CompetitionRepository;
import com.graduation.competition.rule.service.EvidenceFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tool1：竞赛规则检索。自动匹配只选择开放赛事，并过滤已过期规则。
 */
@Component
@RequiredArgsConstructor
public class SearchRulesTool {

    private final CompetitionRepository competitionRepository;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final EvidenceFilterService evidenceFilterService;

    @Tool(name = "searchRules", description = "检索选题相关的有效竞赛规则、组别要求与来源证据")
    public RuleEvidenceBundle searchRules(
            @ToolParam(description = "选题标题或技术设想") String topic,
            @ToolParam(description = "指定竞赛 ID，可为空以自动匹配", required = false) Long competitionId) {
        Competition selected = competitionId == null
                ? autoMatch(topic)
                : competitionRepository.findDetailsById(competitionId).orElse(null);
        List<EvidenceItem> evidence = new ArrayList<>();
        if (selected != null) {
            selected.getRules().stream()
                    .filter(rule -> !rule.isDeleted() && "ACTIVE".equalsIgnoreCase(rule.getStatus()))
                    .limit(6)
                    .forEach(rule -> evidence.add(ruleEvidence(rule, selected)));
        }
        List<Double> vector = embeddingClient.embed(List.of(topic)).get(0);
        List<SearchHit> hits = evidenceFilterService.filterValid(vectorStoreService.search(
                vector, new SearchOptions(6, selected == null ? null : selected.getId(), null, null)));
        hits.forEach(hit -> evidence.add(new EvidenceItem(
                hit.documentId(), hit.chunkIndex(), hit.content(), hit.source(),
                hit.pageNumber(), hit.sourceDate(), hit.score(), "RULE")));
        return new RuleEvidenceBundle(selected, evidence,
                selected == null ? "未匹配到开放竞赛，规则判断缺少明确赛事范围。" : "已匹配开放竞赛：" + selected.getName());
    }

    private Competition autoMatch(String topic) {
        return competitionRepository.findAll().stream()
                .filter(competition -> !competition.isDeleted())
                .filter(competition -> !"EXPIRED".equalsIgnoreCase(competition.getStatus()))
                .filter(competition -> competition.getContestEnd() == null
                        || !competition.getContestEnd().isBefore(LocalDate.now()))
                .map(competition -> java.util.Map.entry(competition, relevance(topic,
                        competition.getName() + " " + value(competition.getDescription()))))
                .max(Comparator.comparingDouble(java.util.Map.Entry::getValue))
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> competitionRepository.findDetailsById(entry.getKey().getId()).orElse(entry.getKey()))
                .orElse(null);
    }

    private double relevance(String topic, String target) {
        String normalized = target.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (int index = 0; index + 2 <= topic.length(); index += 2) {
            if (normalized.contains(topic.substring(index, index + 2))) {
                matched++;
            }
        }
        return (double) matched / Math.max(1, topic.length() / 2);
    }

    private EvidenceItem ruleEvidence(CompetitionRule rule, Competition competition) {
        return new EvidenceItem(
                0,
                rule.getId() == null ? 0 : rule.getId().intValue(),
                "规则：" + rule.getTitle() + "。\n" + rule.getContent(),
                rule.getSourceUrl(),
                null,
                rule.getSourceDate(),
                1.0,
                "RULE");
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

