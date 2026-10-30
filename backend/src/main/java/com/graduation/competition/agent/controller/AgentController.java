package com.graduation.competition.agent.controller;

import com.graduation.competition.agent.dto.AnalyzeRequest;
import com.graduation.competition.agent.service.AnalysisAgentService;
import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.project.dto.ReportResponse;
import com.graduation.competition.project.dto.TraceResponse;
import com.graduation.competition.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AnalysisAgentService analysisAgentService;
    private final ProjectService projectService;

    @PostMapping(value = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyze(@Valid @RequestBody AnalyzeRequest request) {
        return analysisAgentService.analyze(request);
    }

    @GetMapping("/projects/{projectId}/report")
    public ApiResponse<ReportResponse> report(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getLatestReport(projectId));
    }

    @GetMapping("/projects/{projectId}/trace")
    public ApiResponse<List<TraceResponse>> trace(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getTraces(projectId));
    }
}

