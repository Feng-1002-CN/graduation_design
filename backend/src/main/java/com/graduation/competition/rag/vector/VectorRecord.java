package com.graduation.competition.rag.vector;

import java.time.LocalDate;
import java.util.List;

public record VectorRecord(
        String id,
        long documentId,
        Long competitionId,
        String source,
        LocalDate sourceDate,
        Integer pageNumber,
        String content,
        List<Double> vector,
        String documentType,
        String workYear) {
}

