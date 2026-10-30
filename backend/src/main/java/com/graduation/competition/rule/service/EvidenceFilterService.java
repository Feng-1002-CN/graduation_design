package com.graduation.competition.rule.service;

import com.graduation.competition.document.entity.DocumentEntity;
import com.graduation.competition.document.repository.DocumentRepository;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.rag.vector.SearchHit;
import com.graduation.competition.repository.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 证据有效期过滤：所属竞赛已过期或停用时，向量片段不得进入问答和分析报告。
 */
@Service
@RequiredArgsConstructor
public class EvidenceFilterService {

    private final DocumentRepository documentRepository;
    private final CompetitionRepository competitionRepository;

    public List<SearchHit> filterValid(List<SearchHit> hits) {
        if (hits.isEmpty()) {
            return List.of();
        }
        Map<Long, DocumentEntity> documents = documentRepository.findAllById(
                        hits.stream().map(SearchHit::documentId).distinct().toList())
                .stream().collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));
        Map<Long, Competition> competitions = competitionRepository.findAllById(
                        documents.values().stream().map(DocumentEntity::getCompetitionId)
                                .filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Competition::getId, Function.identity()));
        List<SearchHit> valid = new ArrayList<>();
        for (SearchHit hit : hits) {
            DocumentEntity document = documents.get(hit.documentId());
            if (document == null || !"READY".equals(document.getStatus())) {
                continue;
            }
            if (document.getCompetitionId() != null) {
                Competition competition = competitions.get(document.getCompetitionId());
                if (competition == null || competition.isDeleted()
                        || "EXPIRED".equalsIgnoreCase(competition.getStatus())
                        || competition.getContestEnd() != null
                        && competition.getContestEnd().isBefore(LocalDate.now())) {
                    continue;
                }
            }
            valid.add(hit);
        }
        return valid;
    }
}

