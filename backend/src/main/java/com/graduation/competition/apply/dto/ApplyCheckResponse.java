package com.graduation.competition.apply.dto;

import java.time.Instant;
import java.util.List;

public record ApplyCheckResponse(
        Long id,
        Long competitionId,
        List<RequiredItem> required,
        List<CheckItemResult> result,
        Instant createdAt) {
}

