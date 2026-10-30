package com.graduation.competition.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VideoGenerateRequest(
        @NotNull(message = "项目 ID 不能为空") Long projectId,
        @Size(max = 20, message = "最多上传 20 个素材") List<String> images) {
}

