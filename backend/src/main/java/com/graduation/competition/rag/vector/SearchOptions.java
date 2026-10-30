package com.graduation.competition.rag.vector;

import java.time.LocalDate;

public record SearchOptions(
        int topK,
        Long competitionId,
        LocalDate sourceDateFrom,
        String documentType) {
}

