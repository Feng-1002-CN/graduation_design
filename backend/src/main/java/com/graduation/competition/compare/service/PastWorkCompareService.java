package com.graduation.competition.compare.service;

import com.graduation.competition.compare.dto.PastWorkCompareRequest;
import com.graduation.competition.compare.dto.PastWorkCompareResult;
import com.graduation.competition.document.entity.DocumentEntity;
import com.graduation.competition.document.repository.DocumentRepository;
import com.graduation.competition.rag.embedding.EmbeddingClient;
import com.graduation.competition.rag.vector.SearchHit;
import com.graduation.competition.rag.vector.SearchOptions;
import com.graduation.competition.rag.vector.VectorStoreService;
import com.graduation.competition.rule.service.EvidenceFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 往届作品相似检索。重合度是检索相关性的定性解释，不构成抄袭判定或获奖预测。
 */
@Service
@RequiredArgsConstructor
public class PastWorkCompareService {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final EvidenceFilterService evidenceFilterService;
    private final DocumentRepository documentRepository;

    @Value("${app.compare.min-score:0.15}")
    private double minScore;

    @Transactional(readOnly = true)
    public List<PastWorkCompareResult> compare(PastWorkCompareRequest request) {
        List<Double> vector = embeddingClient.embed(List.of(request.topic())).get(0);
        List<SearchHit> hits = evidenceFilterService.filterValid(vectorStoreService.search(
                vector,
                new SearchOptions(Math.max(30, request.resolvedTopK() * 6),
                        request.competitionId(), null, "PAST_WORK")));
        if (hits.isEmpty()) {
            return List.of();
        }
        Map<Long, SearchHit> bestByDocument = new HashMap<>();
        for (SearchHit hit : hits) {
            if (hit.score() >= minScore) {
                bestByDocument.merge(hit.documentId(), hit,
                        (left, right) -> left.score() >= right.score() ? left : right);
            }
        }
        Map<Long, DocumentEntity> documents = documentRepository.findAllById(bestByDocument.keySet())
                .stream().collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));
        return bestByDocument.values().stream()
                .map(hit -> toResult(hit, documents.get(hit.documentId()), request.topic()))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingDouble(PastWorkCompareResult::score).reversed())
                .limit(request.resolvedTopK())
                .toList();
    }

    private PastWorkCompareResult toResult(SearchHit hit, DocumentEntity document, String topic) {
        if (document == null) {
            return null;
        }
        String topicOverlap = level(hit.score(), 0.68, 0.35);
        double termCoverage = termCoverage(topic, hit.content());
        String techOverlap = level(termCoverage, 0.22, 0.08);
        String description = "检索片段与选题的相似度为 " + String.format("%.3f", hit.score())
                + "；主题判断为" + topicOverlap
                + "，技术路线词项覆盖判断为" + techOverlap
                + "。建议核对原作品后，从数据、算法或应用场景形成差异化，不将此结果作为查重或评奖依据。";
        return new PastWorkCompareResult(
                document.getId(),
                document.getTitle(),
                document.getWorkYear(),
                topicOverlap,
                techOverlap,
                description,
                document.getSourceUrl() == null ? document.getFileUrl() : document.getSourceUrl(),
                document.getSourceDate(),
                Math.round(hit.score() * 1000.0) / 1000.0);
    }

    private String level(double score, double high, double medium) {
        return score >= high ? "高" : score >= medium ? "中" : "低";
    }

    private double termCoverage(String topic, String content) {
        if (topic == null || content == null) {
            return 0;
        }
        String normalizedTopic = topic.replaceAll("[\\s，。,.；;：:、]", "");
        int total = 0;
        int matched = 0;
        for (int index = 0; index + 2 <= normalizedTopic.length(); index += 2) {
            String term = normalizedTopic.substring(index, index + 2);
            total++;
            if (content.contains(term)) {
                matched++;
            }
        }
        return total == 0 ? 0 : (double) matched / total;
    }
}

