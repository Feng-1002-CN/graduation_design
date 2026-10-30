package com.graduation.competition.plan.dto;

import jakarta.validation.constraints.NotNull;

public record PlanGenerateRequest(@NotNull(message = "项目 ID 不能为空") Long projectId) {
}

