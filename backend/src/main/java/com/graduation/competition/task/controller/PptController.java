package com.graduation.competition.task.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.storage.ObjectStorageService;
import com.graduation.competition.task.dto.PptGenerateRequest;
import com.graduation.competition.task.entity.GenerationTask;
import com.graduation.competition.task.service.PptGenerateTask;
import com.graduation.competition.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ppt")
@RequiredArgsConstructor
public class PptController {

    private final TaskService taskService;
    private final PptGenerateTask generateTask;
    private final ObjectStorageService storageService;

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@Valid @RequestBody PptGenerateRequest request) {
        GenerationTask task = taskService.createPptTask(request.projectId());
        generateTask.execute(task.getId(), request.templateId());
        return ApiResponse.success(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @GetMapping("/{taskId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long taskId) {
        GenerationTask task = taskService.requireTask(taskId);
        if (!"PPT".equals(task.getTaskType()) || !"SUCCESS".equals(task.getStatus())
                || task.getFileUrl() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "PPT 尚未生成完成");
        }
        InputStreamResource resource = new InputStreamResource(storageService.get(task.getFileUrl()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("project-" + task.getProjectId() + ".pptx", StandardCharsets.UTF_8)
                        .build().toString())
                .body(resource);
    }
}

