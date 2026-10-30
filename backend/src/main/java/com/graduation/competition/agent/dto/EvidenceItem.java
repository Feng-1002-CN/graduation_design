package com.graduation.competition.agent.dto;

import java.time.LocalDate;

public record EvidenceItem(
        long documentId,
        int chunkIndex,
        String content,
        String source,
        Integer pageNumber,
        LocalDate sourceDate,
        double score,
        String category) {
}

