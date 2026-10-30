package com.graduation.competition.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.project.dto.ReportResponse;
import com.graduation.competition.project.dto.TraceResponse;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.AnalysisReportRepository;
import com.graduation.competition.project.repository.AgentTraceRepository;
import com.graduation.competition.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AnalysisReportRepository reportRepository;
    private final AgentTraceRepository traceRepository;

    @Transactional
    public Project createOrGet(Long projectId, String title, String description, Long competitionId) {
        if (projectId != null) {
            return projectRepository.findById(projectId)
                    .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在"));
        }
        Project project = new Project();
        project.setTitle(title);
        project.setDescription(description);
        project.setTargetCompetitionId(competitionId);
        return projectRepository.saveAndFlush(project);
    }

    @Transactional
    public void markAnalyzed(Long projectId) {
        Project project = requireProject(projectId);
        project.setStatus("ANALYZED");
        projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public ReportResponse getLatestReport(Long projectId) {
        requireProject(projectId);
        var report = reportRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目尚无分析报告"));
        try {
            JsonNode refs = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(report.getRefs() == null ? "[]" : report.getRefs());
            return new ReportResponse(report.getId(), projectId, report.getContent(),
                    refs, report.getCreatedAt());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "报告引用数据损坏");
        }
    }

    @Transactional(readOnly = true)
    public List<TraceResponse> getTraces(Long projectId) {
        requireProject(projectId);
        return traceRepository.findByProjectIdOrderByStepAsc(projectId).stream()
                .map(trace -> new TraceResponse(
                        trace.getId(), trace.getStep(), trace.getToolName(), trace.getInput(),
                        abbreviate(trace.getOutput(), 300), trace.getStartedAt(), trace.getEndedAt()))
                .toList();
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在"));
    }

    private String abbreviate(String value, int length) {
        if (value == null) {
            return null;
        }
        return value.length() <= length ? value : value.substring(0, length);
    }
}

