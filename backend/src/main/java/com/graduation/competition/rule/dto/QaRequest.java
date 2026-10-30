package com.graduation.competition.rule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record QaRequest(
        @NotBlank(message = "问题不能为空") String question,
        Long competitionId,
        @Min(1) @Max(20) Integer topK) {

    public int resolvedTopK() {
        return topK == null ? 5 : topK;
    }
}

