package com.graduation.competition.project.dto;

import java.time.Instant;

public record TraceResponse(
        Long id,
        int step,
        String toolName,
        String input,
        String summary,
        Instant startedAt,
        Instant endedAt) {
}

