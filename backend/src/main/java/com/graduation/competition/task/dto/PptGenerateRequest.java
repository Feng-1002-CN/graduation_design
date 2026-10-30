package com.graduation.competition.task.dto;

import jakarta.validation.constraints.NotNull;

public record PptGenerateRequest(
        @NotNull(message = "项目 ID 不能为空") Long projectId,
        String templateId) {
}

