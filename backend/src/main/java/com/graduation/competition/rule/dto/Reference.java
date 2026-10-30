package com.graduation.competition.rule.dto;

import java.time.LocalDate;

public record Reference(
        int index,
        long documentId,
        int chunkIndex,
        String source,
        Integer pageNumber,
        LocalDate sourceDate,
        double score) {
}

