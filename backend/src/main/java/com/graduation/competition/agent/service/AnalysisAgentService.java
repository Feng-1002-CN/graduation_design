package com.graduation.competition.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.agent.dto.AgentEvent;
import com.graduation.competition.agent.dto.AnalyzeRequest;
import com.graduation.competition.agent.dto.EvidenceItem;
import com.graduation.competition.agent.tool.GenerateReportTool;
import com.graduation.competition.agent.tool.GroupMatchResult;
import com.graduation.competition.agent.tool.MatchGroupTool;
import com.graduation.competition.agent.tool.RuleEvidenceBundle;
import com.graduation.competition.agent.tool.SearchPastWorksTool;
import com.graduation.competition.agent.tool.SearchRulesTool;
import com.graduation.competition.compare.dto.PastWorkCompareResult;
import com.graduation.competition.project.entity.AgentTrace;
import com.graduation.competition.project.entity.AnalysisReport;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.AgentTraceRepository;
import com.graduation.competition.project.repository.AnalysisReportRepository;
import com.graduation.competition.project.repository.ProjectRepository;
import com.graduation.competition.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * 固定四步选题分析编排器。工具通过 Spring AI @Tool 声明，执行顺序确定且可审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisAgentService {

    private static final String SYSTEM_PROMPT = """
            你是学科竞赛项目分析助手。依据给定证据输出 Markdown 报告，固定包含：
            规则匹配、技术可行性、项目完整性、相似项目、选题风险、优化建议。
            所有事实性结论必须关联证据来源，资料不足时明确说明。
            禁止预测获奖概率、获奖率或任何评奖结果；禁止编造规则、作品和指标。
            """;

    private final ProjectService projectService;
    private final SearchRulesTool searchRulesTool;
    private final MatchGroupTool matchGroupTool;
    private final SearchPastWorksTool searchPastWorksTool;
    private final GenerateReportTool generateReportTool;
    private final AnalysisReportRepository reportRepository;
    private final AgentTraceRepository traceRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Qualifier("applicationTaskExecutor")
    private final Executor executor;

    public SseEmitter analyze(AnalyzeRequest request) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        executor.execute(() -> execute(request, event -> send(emitter, event), emitter));
        return emitter;
    }

    public void execute(AnalyzeRequest request, Consumer<AgentEvent> consumer) {
        execute(request, consumer, null);
    }

    private void execute(AnalyzeRequest request, Consumer<AgentEvent> consumer, SseEmitter emitter) {
        try {
            Project project = projectService.createOrGet(
                    request.projectId(), request.title(), request.description(), request.competitionId());
            String topic = (project.getTitle() + " " + value(project.getDescription())).trim();

            AgentTrace trace1 = startTrace(project, 1, "searchRules", topic);
            consumer.accept(AgentEvent.stepStart(1, "searchRules", "正在检索对应赛事及组别规则..."));
            RuleEvidenceBundle rules = searchRulesTool.searchRules(topic, project.getTargetCompetitionId());
            for (EvidenceItem evidence : rules.evidence()) {
                consumer.accept(AgentEvent.evidence(1,
                        evidencePayload(evidence, "R" + (rules.evidence().indexOf(evidence) + 1))));
            }
            finishTrace(trace1, "命中证据 " + rules.evidence().size() + " 条");
            consumer.accept(AgentEvent.stepEnd(1, "searchRules",
                    "命中证据 " + rules.evidence().size() + " 条"));

            Long competitionId = rules.competition() == null
                    ? project.getTargetCompetitionId() : rules.competition().getId();
            AgentTrace trace2 = startTrace(project, 2, "matchGroup", topic);
            consumer.accept(AgentEvent.stepStart(2, "matchGroup", "正在分析选题与组别资格匹配..."));
            GroupMatchResult group = matchGroupTool.matchGroup(topic, competitionId);
            group.evidence().forEach(evidence -> consumer.accept(AgentEvent.evidence(2,
                    evidencePayload(evidence, "G1"))));
            finishTrace(trace2, "建议组别：" + group.groupName() + "；匹配度：" + group.matchLevel());
            consumer.accept(AgentEvent.stepEnd(2, "matchGroup",
                    "建议组别：" + group.groupName() + "；匹配度：" + group.matchLevel()));

            AgentTrace trace3 = startTrace(project, 3, "searchPastWorks", topic);
            consumer.accept(AgentEvent.stepStart(3, "searchPastWorks", "正在检索相似往届作品..."));
            List<PastWorkCompareResult> works = searchPastWorksTool.searchPastWorks(topic, competitionId);
            List<EvidenceItem> pastEvidence = searchPastWorksTool.toEvidence(works);
            for (EvidenceItem evidence : pastEvidence) {
                consumer.accept(AgentEvent.evidence(3,
                        evidencePayload(evidence, "P" + (pastEvidence.indexOf(evidence) + 1))));
            }
            finishTrace(trace3, "命中相似作品 " + works.size() + " 个");
            consumer.accept(AgentEvent.stepEnd(3, "searchPastWorks",
                    "命中相似作品 " + works.size() + " 个"));

            AgentTrace trace4 = startTrace(project, 4, "generateReport", topic);
            consumer.accept(AgentEvent.stepStart(4, "generateReport", "正在生成六维度分析报告..."));
            List<EvidenceItem> allEvidence = new ArrayList<>(rules.evidence());
            allEvidence.addAll(group.evidence());
            allEvidence.addAll(pastEvidence);
            String report = generateReportTool.generate(project.getTitle(), project.getDescription(),
                    rules.competition(), rules.evidence(), group, works);
            streamReport(report, consumer);
            AnalysisReport saved = saveReport(project, report, allEvidence);
            finishTrace(trace4, "报告 " + saved.getId() + " 已生成");
            consumer.accept(AgentEvent.stepEnd(4, "generateReport", "六维报告生成完成"));
            projectService.markAnalyzed(project.getId());
            consumer.accept(AgentEvent.done(saved.getId(), project.getId()));
            if (emitter != null) {
                emitter.complete();
            }
        } catch (Exception exception) {
            log.error("选题分析失败", exception);
            consumer.accept(AgentEvent.error(
                    exception.getMessage() == null ? "分析服务异常" : exception.getMessage()));
            if (emitter != null) {
                emitter.complete();
            }
        }
    }

    private void streamReport(String report, Consumer<AgentEvent> consumer) {
        int chunkSize = 90;
        for (int start = 0; start < report.length(); start += chunkSize) {
            consumer.accept(AgentEvent.delta(
                    report.substring(start, Math.min(report.length(), start + chunkSize))));
        }
    }

    private Map<String, Object> evidencePayload(EvidenceItem evidence, String referenceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("referenceId", referenceId);
        payload.put("documentId", evidence.documentId());
        payload.put("source", evidence.source());
        payload.put("pageNumber", evidence.pageNumber());
        payload.put("sourceDate", evidence.sourceDate());
        payload.put("score", evidence.score());
        payload.put("category", evidence.category());
        return payload;
    }

    private AgentTrace startTrace(Project project, int step, String tool, String input) {
        AgentTrace trace = new AgentTrace();
        trace.setProjectId(project.getId());
        trace.setStep(step);
        trace.setToolName(tool);
        trace.setInput(abbreviate(input, 4000));
        trace.setStartedAt(Instant.now());
        return traceRepository.save(trace);
    }

    private void finishTrace(AgentTrace trace, String output) {
        trace.setOutput(abbreviate(output, 10000));
        trace.setEndedAt(Instant.now());
        traceRepository.save(trace);
    }

    private AnalysisReport saveReport(Project project, String content, List<EvidenceItem> evidence) {
        try {
            AnalysisReport report = new AnalysisReport();
            report.setProjectId(project.getId());
            report.setContent(content);
            List<Map<String, Object>> refs = evidence.stream().map(item -> {
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("documentId", item.documentId());
                ref.put("source", item.source());
                ref.put("pageNumber", item.pageNumber());
                ref.put("sourceDate", item.sourceDate());
                ref.put("score", item.score());
                ref.put("category", item.category());
                return ref;
            }).toList();
            report.setRefs(objectMapper.writeValueAsString(refs));
            return reportRepository.saveAndFlush(report);
        } catch (Exception exception) {
            throw new IllegalStateException("报告持久化失败", exception);
        }
    }

    private void send(SseEmitter emitter, AgentEvent event) {
        try {
            emitter.send(SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException exception) {
            log.debug("SSE 客户端已断开: {}", exception.getMessage());
        }
    }

    private String abbreviate(String value, int length) {
        if (value == null) {
            return "";
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

