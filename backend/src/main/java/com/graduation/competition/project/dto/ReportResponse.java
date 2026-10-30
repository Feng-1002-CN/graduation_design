package com.graduation.competition.project.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record ReportResponse(
        Long reportId,
        Long projectId,
        String content,
        JsonNode refs,
        Instant createdAt) {
}

