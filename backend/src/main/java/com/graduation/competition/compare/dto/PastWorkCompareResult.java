package com.graduation.competition.compare.dto;

import java.time.LocalDate;

public record PastWorkCompareResult(
        Long documentId,
        String title,
        String workYear,
        String topicOverlap,
        String techOverlap,
        String overlapDesc,
        String source,
        LocalDate sourceDate,
        double score) {
}

