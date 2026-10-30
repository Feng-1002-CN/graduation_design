package com.graduation.competition.rag.vector;

import java.time.LocalDate;

public record SearchHit(
        long documentId,
        int chunkIndex,
        Integer pageNumber,
        String content,
        String source,
        LocalDate sourceDate,
        double score,
        String documentType,
        String workYear) {
}

