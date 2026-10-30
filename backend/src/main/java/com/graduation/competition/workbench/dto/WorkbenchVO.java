package com.graduation.competition.workbench.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record WorkbenchVO(
        ProjectSummary project,
        ReportSummary latestReport,
        PlanSummary plan,
        List<TaskSummary> tasks,
        ApplicationSummary applyCheck,
        List<TraceSummary> traces,
        List<TimelineEvent> timeline) {

    public record ProjectSummary(
            Long id,
            String title,
            String description,
            Long targetCompetitionId,
            String status,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ReportSummary(
            Long reportId,
            String summary,
            Instant createdAt) {
    }

    public record PlanSummary(
            Long id,
            String title,
            String modules,
            String techRoute,
            Instant updatedAt) {
    }

    public record TaskSummary(
            Long taskId,
            String taskType,
            String status,
            String stage,
            int progress,
            String fileUrl,
            String errorMsg,
            Instant updatedAt) {
    }

    public record ApplicationSummary(
            Long id,
            JsonNode result,
            Instant createdAt) {
    }

    public record TraceSummary(
            Long id,
            int step,
            String toolName,
            String summary,
            Instant startedAt,
            Instant endedAt) {
    }

    public record TimelineEvent(
            Instant time,
            String type,
            String title,
            String status) {
    }
}

