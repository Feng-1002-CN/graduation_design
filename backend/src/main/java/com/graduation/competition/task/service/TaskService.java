package com.graduation.competition.task.service;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.project.repository.ProjectRepository;
import com.graduation.competition.task.dto.TaskResponse;
import com.graduation.competition.task.entity.GenerationTask;
import com.graduation.competition.task.repository.GenerationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final GenerationTaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public GenerationTask createPptTask(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在");
        }
        GenerationTask task = new GenerationTask();
        task.setProjectId(projectId);
        task.setTaskType("PPT");
        task.setStatus("PENDING");
        task.setStage("QUEUED");
        task.setProgress(0);
        return taskRepository.saveAndFlush(task);
    }

    @Transactional
    public GenerationTask createVideoTask(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在");
        }
        GenerationTask task = new GenerationTask();
        task.setProjectId(projectId);
        task.setTaskType("VIDEO");
        task.setStatus("PENDING");
        task.setStage("QUEUED");
        task.setProgress(0);
        return taskRepository.saveAndFlush(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long taskId) {
        return TaskResponse.from(requireTask(taskId));
    }

    @Transactional(readOnly = true)
    public GenerationTask requireTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "任务不存在"));
    }

    @Transactional
    public void update(Long taskId, String status, String stage, int progress,
                       String fileUrl, String errorMsg) {
        GenerationTask task = requireTask(taskId);
        task.setStatus(status);
        task.setStage(stage);
        task.setProgress(progress);
        task.setFileUrl(fileUrl);
        task.setErrorMsg(errorMsg);
        taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listByProject(Long projectId) {
        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(TaskResponse::from).toList();
    }
}

