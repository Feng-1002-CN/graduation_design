package com.graduation.competition.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.ai.AiGateway;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.rag.embedding.EmbeddingClient;
import com.graduation.competition.rag.vector.SearchHit;
import com.graduation.competition.rag.vector.SearchOptions;
import com.graduation.competition.rag.vector.VectorStoreService;
import com.graduation.competition.repository.CompetitionRepository;
import com.graduation.competition.rule.dto.QaRequest;
import com.graduation.competition.rule.dto.QaResponse;
import com.graduation.competition.rule.dto.Reference;
import com.graduation.competition.rule.entity.QaLog;
import com.graduation.competition.rule.repository.QaLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 单轮 RAG 规则问答。回答必须逐条引用证据，资料不足时明确降级。
 */
@Service
@RequiredArgsConstructor
public class RuleQaService {

    private static final String SYSTEM_PROMPT = """
            你是学科竞赛规则助手。只能依据用户消息中【证据】列出的片段回答。
            每个关键结论后必须标注 [序号]，序号对应证据编号。
            禁止编造未出现的要求，禁止预测获奖概率、获奖率或任何评奖结果。
            如果证据不足，必须明确回答“未检索到可支持的规则依据”，不得补全。
            """;

    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final EvidenceFilterService evidenceFilterService;
    private final CompetitionRepository competitionRepository;
    private final AiGateway aiGateway;
    private final QaLogRepository qaLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.qa.min-score:0.05}")
    private double minScore;

    @Transactional
    public QaResponse ask(QaRequest request) {
        validateCompetition(request.competitionId());
        List<Double> vector = embeddingClient.embed(List.of(request.question())).get(0);
        List<SearchHit> hits = evidenceFilterService.filterValid(
                vectorStoreService.search(vector, new SearchOptions(
                        request.resolvedTopK(), request.competitionId(), null, null)));
        if (hits.isEmpty() || hits.get(0).score() < minScore) {
            QaResponse response = new QaResponse(
                    "未检索到可支持回答的规则依据。请先上传对应竞赛的有效资料。",
                    List.of(), true);
            saveLog(request, response);
            return response;
        }
        String evidence = buildEvidence(hits);
        String answer = aiGateway.complete(SYSTEM_PROMPT,
                "问题：" + request.question() + "\n\n【证据】\n" + evidence);
        if (!answer.contains("[1]")) {
            answer += "\n\n可核对证据：" + hits.stream()
                    .map(hit -> "[" + (hits.indexOf(hit) + 1) + "]")
                    .collect(Collectors.joining(" "));
        }
        QaResponse response = new QaResponse(answer, references(hits), false);
        saveLog(request, response);
        return response;
    }

    private String buildEvidence(List<SearchHit> hits) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < hits.size(); index++) {
            SearchHit hit = hits.get(index);
            builder.append("[").append(index + 1).append("] ")
                    .append("来源=").append(hit.source())
                    .append("；页=").append(hit.pageNumber())
                    .append("；发布日期=").append(hit.sourceDate())
                    .append("\n").append(hit.content()).append("\n\n");
        }
        return builder.toString();
    }

    private List<Reference> references(List<SearchHit> hits) {
        return java.util.stream.IntStream.range(0, hits.size())
                .mapToObj(index -> {
                    SearchHit hit = hits.get(index);
                    return new Reference(index + 1, hit.documentId(), hit.chunkIndex(),
                            hit.source(), hit.pageNumber(), hit.sourceDate(), hit.score());
                }).toList();
    }

    private void validateCompetition(Long competitionId) {
        if (competitionId == null) {
            return;
        }
        Competition competition = competitionRepository.findByIdAndDeletedFalse(competitionId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "竞赛不存在"));
        if ("EXPIRED".equalsIgnoreCase(competition.getStatus())
                || competition.getContestEnd() != null
                && competition.getContestEnd().isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该竞赛已过期，不能用于当前规则问答");
        }
    }

    private void saveLog(QaRequest request, QaResponse response) {
        try {
            QaLog log = new QaLog();
            log.setQuestion(request.question());
            log.setAnswer(response.answer());
            log.setRefs(objectMapper.writeValueAsString(response.references()));
            log.setCompetitionId(request.competitionId());
            qaLogRepository.save(log);
        } catch (Exception ignored) {
            // 审计日志失败不阻断用户问答。
        }
    }
}

