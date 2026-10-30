package com.graduation.competition.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CompetitionResponse(
        Long id,
        String name,
        String organizer,
        String category,
        String level,
        String officialWebsite,
        LocalDate registerStart,
        LocalDate registerEnd,
        LocalDate contestStart,
        LocalDate contestEnd,
        String description,
        String sourceUrl,
        LocalDate sourceDate,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<GroupResponse> groups,
        List<RuleResponse> rules) {

    public record GroupResponse(Long id, String name, String eligibility) {
    }

    public record RuleResponse(
            Long id,
            Long groupId,
            String groupName,
            String title,
            String content,
            String sourceUrl,
            LocalDate sourceDate,
            LocalDate publishDate,
            String status) {
    }
}

