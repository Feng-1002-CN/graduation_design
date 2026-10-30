package com.graduation.competition.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.gen.video.ScriptGenerator;
import com.graduation.competition.gen.video.VideoComposeService;
import com.graduation.competition.gen.video.VideoScene;
import com.graduation.competition.storage.ObjectStorageService;
import com.graduation.competition.task.entity.GenerationTask;
import com.graduation.competition.task.entity.VideoScript;
import com.graduation.competition.task.repository.VideoScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 异步视频任务。严格限制为脚本、TTS 与用户素材合成，不调用视频生成模型。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerateTask {

    private final TaskService taskService;
    private final ScriptGenerator scriptGenerator;
    private final VideoComposeService composeService;
    private final VideoScriptRepository scriptRepository;
    private final ObjectStorageService storageService;
    private final ObjectMapper objectMapper;

    @Async("applicationTaskExecutor")
    public void execute(Long taskId, Long projectId, List<String> images) {
        GenerationTask task = taskService.requireTask(taskId);
        Path workspace = null;
        try {
            taskService.update(taskId, "RUNNING", "SCRIPT", 10, null, null);
            List<VideoScene> scenes = scriptGenerator.generate(projectId, images);
            VideoScript script = new VideoScript();
            script.setTaskId(taskId);
            script.setScenes(objectMapper.writeValueAsString(scenes));
            script.setFullText(scenes.stream().map(VideoScene::text)
                    .collect(java.util.stream.Collectors.joining("\n")));
            scriptRepository.saveAndFlush(script);

            taskService.update(taskId, "RUNNING", "TTS", 35, null, null);
            workspace = Files.createTempDirectory("competition-video-");
            Path output = composeService.compose(scenes, workspace);
            taskService.update(taskId, "RUNNING", "COMPOSING", 85, null, null);

            String objectName = "video/project_" + task.getProjectId() + "_" + UUID.randomUUID() + ".mp4";
            String fileUrl = storageService.put(objectName, "video/mp4", Files.readAllBytes(output));
            taskService.update(taskId, "SUCCESS", "DONE", 100, fileUrl, null);
        } catch (Exception exception) {
            log.error("视频任务 {} 生成失败", taskId, exception);
            taskService.update(taskId, "FAILED", "FAILED", 100, null,
                    exception.getMessage() == null ? "视频生成失败" : exception.getMessage());
        } finally {
            cleanup(workspace);
        }
    }

    private void cleanup(Path workspace) {
        if (workspace == null) {
            return;
        }
        try (var paths = Files.walk(workspace)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}

