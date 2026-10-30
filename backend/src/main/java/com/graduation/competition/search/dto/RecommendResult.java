package com.graduation.competition.search.dto;

import java.time.LocalDate;
import java.util.List;

public record RecommendResult(
        Long competitionId,
        String name,
        String level,
        String reason,
        List<String> matchedKeywords,
        String source,
        LocalDate sourceDate,
        double score) {
}

