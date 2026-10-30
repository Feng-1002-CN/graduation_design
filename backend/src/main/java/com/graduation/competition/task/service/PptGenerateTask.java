package com.graduation.competition.task.service;

import com.graduation.competition.gen.ppt.PptGenerator;
import com.graduation.competition.plan.dto.PlanDTO;
import com.graduation.competition.plan.service.PlanService;
import com.graduation.competition.storage.ObjectStorageService;
import com.graduation.competition.task.entity.GenerationTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PPT 异步生成任务，任务状态持续写回 generation_task。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PptGenerateTask {

    private final TaskService taskService;
    private final PlanService planService;
    private final PptGenerator generator;
    private final ObjectStorageService storageService;

    @Async("applicationTaskExecutor")
    public void execute(Long taskId, String templateId) {
        GenerationTask task = taskService.requireTask(taskId);
        try {
            taskService.update(taskId, "RUNNING", "RENDERING", 20, null, null);
            PlanDTO plan = planService.getByProjectId(task.getProjectId());
            byte[] bytes = generator.generate(plan, templateId);
            taskService.update(taskId, "RUNNING", "UPLOADING", 75, null, null);
            String objectName = "ppt/project_" + task.getProjectId() + "_" + UUID.randomUUID() + ".pptx";
            String fileUrl = storageService.put(objectName,
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation", bytes);
            taskService.update(taskId, "SUCCESS", "DONE", 100, fileUrl, null);
        } catch (Exception exception) {
            log.error("PPT 任务 {} 生成失败", taskId, exception);
            taskService.update(taskId, "FAILED", "FAILED", 100, null,
                    exception.getMessage() == null ? "PPT 生成失败" : exception.getMessage());
        }
    }
}

