package com.graduation.competition.search.service;

import com.graduation.competition.document.entity.DocumentEntity;
import com.graduation.competition.document.repository.DocumentRepository;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.entity.CompetitionGroup;
import com.graduation.competition.rag.embedding.EmbeddingClient;
import com.graduation.competition.rag.vector.SearchHit;
import com.graduation.competition.rag.vector.SearchOptions;
import com.graduation.competition.rag.vector.VectorStoreService;
import com.graduation.competition.repository.CompetitionRepository;
import com.graduation.competition.rule.service.EvidenceFilterService;
import com.graduation.competition.search.dto.RecommendRequest;
import com.graduation.competition.search.dto.RecommendResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 竞赛推荐融合结构化画像匹配与知识片段向量命中，不把相关性解释为获奖概率。
 */
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final CompetitionRepository competitionRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final EvidenceFilterService evidenceFilterService;

    @Transactional(readOnly = true)
    public List<RecommendResult> recommend(RecommendRequest request) {
        List<Competition> candidates = competitionRepository.findAll().stream()
                .filter(competition -> !competition.isDeleted())
                .filter(this::isOpen)
                .toList();
        if (candidates.isEmpty() || !StringUtils.hasText(request.queryText())) {
            return List.of();
        }

        Map<Long, Double> vectorScores = vectorScores(request.queryText());
        List<RecommendResult> results = candidates.stream()
                .map(competition -> score(competition, request, vectorScores.getOrDefault(competition.getId(), 0.0)))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingDouble(RecommendResult::score).reversed())
                .limit(request.resolvedLimit())
                .toList();
        return results;
    }

    private Map<Long, Double> vectorScores(String query) {
        List<Double> vector = embeddingClient.embed(List.of(query)).get(0);
        List<SearchHit> hits = evidenceFilterService.filterValid(
                vectorStoreService.search(vector, new SearchOptions(50, null, null, null)));
        Map<Long, DocumentEntity> documents = documentRepository.findAllById(
                        hits.stream().map(SearchHit::documentId).distinct().toList())
                .stream().collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));
        Map<Long, Double> scores = new HashMap<>();
        for (SearchHit hit : hits) {
            DocumentEntity document = documents.get(hit.documentId());
            if (document != null && document.getCompetitionId() != null) {
                scores.merge(document.getCompetitionId(), Math.max(0, hit.score()), Math::max);
            }
        }
        return scores;
    }

    private RecommendResult score(
            Competition competition,
            RecommendRequest request,
            double vectorScore) {
        String haystack = String.join(" ",
                value(competition.getName()),
                value(competition.getDescription()),
                value(competition.getCategory()),
                value(competition.getLevel()),
                competition.getGroups().stream().map(CompetitionGroup::getName)
                        .collect(Collectors.joining(" "))).toLowerCase(Locale.ROOT);
        Set<String> matched = new LinkedHashSet<>();
        double structured = 0;
        if (StringUtils.hasText(request.direction())
                && containsRelated(haystack, request.direction())) {
            structured += 0.42;
            matched.add(request.direction().trim());
        }
        if (request.keywords() != null) {
            for (String keyword : request.keywords()) {
                if (StringUtils.hasText(keyword) && containsRelated(haystack, keyword)) {
                    structured += 0.12;
                    matched.add(keyword.trim());
                }
            }
        }
        if (StringUtils.hasText(request.grade()) && competition.getGroups().stream()
                .map(CompetitionGroup::getEligibility)
                .filter(Objects::nonNull)
                .anyMatch(value -> value.contains(request.grade()))) {
            structured += 0.2;
            matched.add(request.grade().trim());
        }
        double score = Math.min(1.0, structured + Math.max(0, vectorScore) * 0.35);
        if (score == 0) {
            return new RecommendResult(competition.getId(), competition.getName(), competition.getLevel(),
                    "", List.of(), competition.getSourceUrl(), competition.getSourceDate(), 0);
        }
        String reason = buildReason(request, matched, vectorScore);
        return new RecommendResult(
                competition.getId(), competition.getName(), competition.getLevel(), reason,
                List.copyOf(matched), competition.getSourceUrl(), competition.getSourceDate(),
                Math.round(score * 1000.0) / 1000.0);
    }

    private boolean containsRelated(String haystack, String term) {
        String normalized = term.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return false;
        }
        if (haystack.contains(normalized)) {
            return true;
        }
        for (int index = 0; index + 2 <= normalized.length(); index++) {
            if (haystack.contains(normalized.substring(index, index + 2))) {
                return true;
            }
        }
        return false;
    }

    private String buildReason(RecommendRequest request, Set<String> matched, double vectorScore) {
        String topics = matched.isEmpty()
                ? value(request.direction()) + " " + String.join(" ", request.keywords() == null ? List.of() : request.keywords())
                : String.join("、", matched);
        String basis = vectorScore > 0
                ? "，且知识库资料存在语义相关片段"
                : "";
        return "你关注的“" + topics.trim() + "”与赛事简介或组别要求存在匹配" + basis
                + "；建议按官方规则进一步核验参赛资格与作品要求。";
    }

    private boolean isOpen(Competition competition) {
        return !"EXPIRED".equalsIgnoreCase(competition.getStatus())
                && (competition.getContestEnd() == null
                || !competition.getContestEnd().isBefore(LocalDate.now()))
                && (competition.getRegisterEnd() == null
                || !competition.getRegisterEnd().isBefore(LocalDate.now()));
    }

    private String value(String input) {
        return input == null ? "" : input;
    }
}

