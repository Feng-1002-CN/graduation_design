package com.graduation.competition.task.dto;

import com.graduation.competition.task.entity.GenerationTask;

import java.time.Instant;

public record TaskResponse(
        Long taskId,
        Long projectId,
        String taskType,
        String status,
        String stage,
        int progress,
        String fileUrl,
        String errorMsg,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(GenerationTask task) {
        return new TaskResponse(task.getId(), task.getProjectId(), task.getTaskType(),
                task.getStatus(), task.getStage(), task.getProgress(), task.getFileUrl(),
                task.getErrorMsg(), task.getCreatedAt(), task.getUpdatedAt());
    }
}

