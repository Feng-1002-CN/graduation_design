package com.graduation.competition.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SearchRequest(
        @NotBlank(message = "检索问题不能为空") String query,
        @Min(1) @Max(50) Integer topK,
        Long competitionId,
        LocalDate sourceDateFrom,
        String documentType) {

    public int resolvedTopK() {
        return topK == null ? 5 : topK;
    }
}

