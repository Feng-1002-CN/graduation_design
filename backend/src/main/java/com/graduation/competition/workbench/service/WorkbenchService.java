package com.graduation.competition.workbench.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.apply.repository.ApplicationCheckRepository;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.common.PageResult;
import com.graduation.competition.plan.repository.ProjectPlanRepository;
import com.graduation.competition.project.entity.AgentTrace;
import com.graduation.competition.project.entity.AnalysisReport;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.AgentTraceRepository;
import com.graduation.competition.project.repository.AnalysisReportRepository;
import com.graduation.competition.project.repository.ProjectRepository;
import com.graduation.competition.task.entity.GenerationTask;
import com.graduation.competition.task.repository.GenerationTaskRepository;
import com.graduation.competition.workbench.dto.WorkbenchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 工作台只读聚合器。每个模块独立降级，避免单模块异常拖垮整个页面。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbenchService {

    private final ProjectRepository projectRepository;
    private final AnalysisReportRepository reportRepository;
    private final ProjectPlanRepository planRepository;
    private final GenerationTaskRepository taskRepository;
    private final ApplicationCheckRepository applyRepository;
    private final AgentTraceRepository traceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public WorkbenchVO get(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在"));
        List<GenerationTask> tasks = safeTasks(projectId);
        List<AgentTrace> traces = safeTraces(projectId);
        WorkbenchVO.ReportSummary report = safeReport(projectId);
        WorkbenchVO.PlanSummary plan = safePlan(projectId);
        WorkbenchVO.ApplicationSummary application = safeApplication(project);

        List<WorkbenchVO.TraceSummary> traceViews = traces.stream()
                .map(trace -> new WorkbenchVO.TraceSummary(
                        trace.getId(), trace.getStep(), trace.getToolName(),
                        abbreviate(trace.getOutput(), 180),
                        trace.getStartedAt(), trace.getEndedAt()))
                .toList();
        List<WorkbenchVO.TaskSummary> taskViews = tasks.stream()
                .map(task -> new WorkbenchVO.TaskSummary(
                        task.getId(), task.getTaskType(), task.getStatus(), task.getStage(),
                        task.getProgress(), task.getFileUrl(), task.getErrorMsg(), task.getUpdatedAt()))
                .toList();
        return new WorkbenchVO(
                projectSummary(project),
                report,
                plan,
                taskViews,
                application,
                traceViews,
                timeline(tasks, traces));
    }

    @Transactional(readOnly = true)
    public PageResult<WorkbenchVO.ProjectSummary> list(int page, int size) {
        Page<Project> projects = projectRepository.findAll(PageRequest.of(
                Math.max(0, page - 1),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")));
        return new PageResult<>(projects.getContent().stream()
                .map(this::projectSummary).toList(),
                projects.getTotalElements(), Math.max(page, 1), Math.min(Math.max(size, 1), 100));
    }

    private WorkbenchVO.ProjectSummary projectSummary(Project project) {
        return new WorkbenchVO.ProjectSummary(
                project.getId(), project.getTitle(), project.getDescription(),
                project.getTargetCompetitionId(), project.getStatus(),
                project.getCreatedAt(), project.getUpdatedAt());
    }

    private WorkbenchVO.ReportSummary safeReport(Long projectId) {
        try {
            return reportRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                    .map(report -> new WorkbenchVO.ReportSummary(
                            report.getId(), summarize(report), report.getCreatedAt()))
                    .orElse(null);
        } catch (Exception exception) {
            log.warn("工作台报告读取失败，已降级: {}", exception.getMessage());
            return null;
        }
    }

    private WorkbenchVO.PlanSummary safePlan(Long projectId) {
        try {
            return planRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                    .map(plan -> new WorkbenchVO.PlanSummary(plan.getId(), plan.getTitle(),
                            plan.getModules(), plan.getTechRoute(), plan.getUpdatedAt()))
                    .orElse(null);
        } catch (Exception exception) {
            log.warn("工作台方案读取失败，已降级: {}", exception.getMessage());
            return null;
        }
    }

    private WorkbenchVO.ApplicationSummary safeApplication(Project project) {
        if (project.getTargetCompetitionId() == null) {
            return null;
        }
        try {
            return applyRepository.findTopByCompetitionIdOrderByCreatedAtDesc(
                            project.getTargetCompetitionId())
                    .map(check -> {
                        try {
                            return new WorkbenchVO.ApplicationSummary(check.getId(),
                                    objectMapper.readTree(check.getResult()), check.getCreatedAt());
                        } catch (Exception exception) {
                            return new WorkbenchVO.ApplicationSummary(check.getId(), null,
                                    check.getCreatedAt());
                        }
                    }).orElse(null);
        } catch (Exception exception) {
            log.warn("工作台申报检查读取失败，已降级: {}", exception.getMessage());
            return null;
        }
    }

    private List<GenerationTask> safeTasks(Long projectId) {
        try {
            return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        } catch (Exception exception) {
            log.warn("工作台任务读取失败，已降级: {}", exception.getMessage());
            return List.of();
        }
    }

    private List<AgentTrace> safeTraces(Long projectId) {
        try {
            return traceRepository.findByProjectIdOrderByStepAsc(projectId);
        } catch (Exception exception) {
            log.warn("工作台轨迹读取失败，已降级: {}", exception.getMessage());
            return List.of();
        }
    }

    private List<WorkbenchVO.TimelineEvent> timeline(
            List<GenerationTask> tasks,
            List<AgentTrace> traces) {
        List<WorkbenchVO.TimelineEvent> events = new ArrayList<>();
        for (GenerationTask task : tasks) {
            events.add(new WorkbenchVO.TimelineEvent(task.getUpdatedAt(), "TASK",
                    task.getTaskType() + " 任务 #" + task.getId(), task.getStatus()));
        }
        for (AgentTrace trace : traces) {
            events.add(new WorkbenchVO.TimelineEvent(
                    trace.getEndedAt() == null ? trace.getStartedAt() : trace.getEndedAt(),
                    "TRACE", "步骤 " + trace.getStep() + " · " + trace.getToolName(), "COMPLETED"));
        }
        return events.stream()
                .filter(event -> event.time() != null)
                .sorted(Comparator.comparing(WorkbenchVO.TimelineEvent::time).reversed())
                .toList();
    }

    private String summarize(AnalysisReport report) {
        String content = report.getContent()
                .replaceAll("(?m)^#+\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
        return abbreviate(content, 260);
    }

    private String abbreviate(String value, int length) {
        if (value == null) {
            return null;
        }
        return value.length() <= length ? value : value.substring(0, length) + "…";
    }
}

