package com.graduation.competition.agent.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record AnalyzeRequest(
        Long projectId,
        @Size(max = 255) String title,
        @Size(max = 10000) String description,
        Long competitionId) {

    @AssertTrue(message = "新项目必须填写选题标题")
    public boolean isTitlePresentForNewProject() {
        return projectId != null || title != null && !title.isBlank();
    }
}

