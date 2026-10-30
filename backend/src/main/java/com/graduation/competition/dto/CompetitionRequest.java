package com.graduation.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * 竞赛新增/编辑请求。name、sourceUrl 与 sourceDate 为强制元数据。
 */
public record CompetitionRequest(
        @NotBlank(message = "竞赛名称不能为空") String name,
        String organizer,
        String category,
        String level,
        String officialWebsite,
        LocalDate registerStart,
        LocalDate registerEnd,
        LocalDate contestStart,
        LocalDate contestEnd,
        String description,
        @NotBlank(message = "来源 URL 不能为空") String sourceUrl,
        @NotNull(message = "来源发布日期不能为空") LocalDate sourceDate,
        String status,
        @Valid List<GroupRequest> groups,
        @Valid List<RuleRequest> rules) {

    public record GroupRequest(
            @NotBlank(message = "组别名称不能为空") String name,
            String eligibility) {
    }

    public record RuleRequest(
            Long groupId,
            @NotBlank(message = "规则标题不能为空") String title,
            @NotBlank(message = "规则正文不能为空") String content,
            @NotBlank(message = "规则来源 URL 不能为空") String sourceUrl,
            @NotNull(message = "规则来源发布日期不能为空") LocalDate sourceDate,
            LocalDate publishDate,
            String status) {
    }
}

