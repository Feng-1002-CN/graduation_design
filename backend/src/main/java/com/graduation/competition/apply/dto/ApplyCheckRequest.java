package com.graduation.competition.apply.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApplyCheckRequest(
        @NotNull(message = "竞赛 ID 不能为空") Long competitionId,
        @Valid List<ApplyFile> files) {
}

