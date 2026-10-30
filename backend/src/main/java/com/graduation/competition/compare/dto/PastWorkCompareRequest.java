package com.graduation.competition.compare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PastWorkCompareRequest(
        @NotBlank(message = "选题描述不能为空") String topic,
        Long competitionId,
        @Min(1) @Max(20) Integer topK) {

    public int resolvedTopK() {
        return topK == null ? 5 : topK;
    }
}

