package com.graduation.competition.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentEvent(
        String type,
        Integer step,
        String tool,
        String message,
        Object data,
        String summary,
        String content,
        Long reportId,
        Long projectId) {

    public static AgentEvent stepStart(int step, String tool, String message) {
        return new AgentEvent("step_start", step, tool, message, null, null, null, null, null);
    }

    public static AgentEvent evidence(int step, Object data) {
        return new AgentEvent("evidence", step, null, null, data, null, null, null, null);
    }

    public static AgentEvent stepEnd(int step, String tool, String summary) {
        return new AgentEvent("step_end", step, tool, null, null, summary, null, null, null);
    }

    public static AgentEvent delta(String content) {
        return new AgentEvent("delta", null, null, null, null, null, content, null, null);
    }

    public static AgentEvent done(Long reportId, Long projectId) {
        return new AgentEvent("done", null, null, null, null, null, null, reportId, projectId);
    }

    public static AgentEvent error(String message) {
        return new AgentEvent("error", null, null, message, null, null, null, null, null);
    }
}

