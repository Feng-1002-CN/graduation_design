package com.graduation.competition.task.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.storage.ObjectStorageService;
import com.graduation.competition.task.dto.VideoGenerateRequest;
import com.graduation.competition.task.entity.GenerationTask;
import com.graduation.competition.task.service.TaskService;
import com.graduation.competition.task.service.VideoGenerateTask;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
public class VideoController {

    private final TaskService taskService;
    private final VideoGenerateTask generateTask;
    private final ObjectStorageService storageService;

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(
            @Valid @RequestBody VideoGenerateRequest request) {
        GenerationTask task = taskService.createVideoTask(request.projectId());
        generateTask.execute(task.getId(), request.projectId(), request.images());
        return ApiResponse.success(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @PostMapping(value = "/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(
            @RequestPart("file") MultipartFile file) throws Exception {
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!Set.of("image/png", "image/jpeg", "image/webp").contains(contentType)) {
            throw new BizException(ErrorCode.INVALID_FILE, "仅支持 PNG、JPEG 或 WebP 图片");
        }
        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        String url = storageService.put("material/" + UUID.randomUUID() + "." + extension,
                contentType, file.getBytes());
        return ApiResponse.success(Map.of("fileUrl", url));
    }

    @GetMapping("/{taskId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long taskId) {
        GenerationTask task = taskService.requireTask(taskId);
        if (!"VIDEO".equals(task.getTaskType()) || !"SUCCESS".equals(task.getStatus())
                || task.getFileUrl() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "视频尚未生成完成");
        }
        InputStreamResource resource = new InputStreamResource(storageService.get(task.getFileUrl()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("project-" + task.getProjectId() + ".mp4", StandardCharsets.UTF_8)
                        .build().toString())
                .body(resource);
    }
}

