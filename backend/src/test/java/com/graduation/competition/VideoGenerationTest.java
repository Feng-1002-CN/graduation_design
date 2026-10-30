package com.graduation.competition;

import com.graduation.competition.gen.video.FfmpegService;
import com.graduation.competition.gen.video.ScriptGenerator;
import com.graduation.competition.gen.video.VideoScene;
import com.graduation.competition.plan.service.PlanService;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.ProjectRepository;
import com.graduation.competition.task.entity.GenerationTask;
import com.graduation.competition.task.entity.VideoScript;
import com.graduation.competition.task.repository.GenerationTaskRepository;
import com.graduation.competition.task.repository.VideoScriptRepository;
import com.graduation.competition.task.service.TaskService;
import com.graduation.competition.task.service.VideoGenerateTask;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class VideoGenerationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PlanService planService;

    @Autowired
    private ScriptGenerator scriptGenerator;

    @Autowired
    private TaskService taskService;

    @Autowired
    private VideoGenerateTask generateTask;

    @Autowired
    private GenerationTaskRepository taskRepository;

    @Autowired
    private VideoScriptRepository scriptRepository;

    @Autowired
    private FfmpegService ffmpegService;

    @Autowired
    private com.graduation.competition.storage.ObjectStorageService storageService;

    @Test
    void createsStructuredScriptAndPlayableMp4WhenFfmpegAvailable() throws Exception {
        try {
            ffmpegService.ensureAvailable();
        } catch (Exception exception) {
            Assumptions.assumeTrue(false,
                    "当前测试机未安装 FFmpeg，跳过真实视频合成: " + exception.getMessage());
        }

        Project project = new Project();
        project.setTitle("智能中药材识别展示项目");
        project.setDescription("基于图像识别，展示数据采集、分类、复核与结果导出。");
        project = projectRepository.save(project);
        planService.generate(project.getId());

        List<VideoScene> scenes = scriptGenerator.generate(project.getId(), List.of());
        assertThat(scenes).hasSizeGreaterThanOrEqualTo(3);
        assertThat(scenes).allMatch(scene -> scene.text() != null && scene.durationSec() > 0);

        GenerationTask task = taskService.createVideoTask(project.getId());
        generateTask.execute(task.getId(), project.getId(), List.of());
        GenerationTask completed = awaitTask(task.getId());
        assertThat(completed.getStatus()).isEqualTo("SUCCESS");
        assertThat(completed.getStage()).isEqualTo("DONE");
        assertThat(completed.getProgress()).isEqualTo(100);

        byte[] mp4;
        try (var input = storageService.get(completed.getFileUrl())) {
            mp4 = input.readAllBytes();
        }
        assertThat(mp4.length).isGreaterThan(1000);
        assertThat(new String(mp4, 0, Math.min(32, mp4.length), StandardCharsets.ISO_8859_1))
                .contains("ftyp");
        Path media = Files.createTempFile("competition-video-", ".mp4");
        Files.write(media, mp4);
        Path ffmpeg = Path.of(System.getenv().getOrDefault("FFMPEG_EXECUTABLE", "ffmpeg"));
        Path ffprobe = ffmpeg.resolveSibling(
                System.getProperty("os.name").toLowerCase().contains("win") ? "ffprobe.exe" : "ffprobe");
        Process probe = new ProcessBuilder(ffprobe.toString(), "-v", "error", "-show_entries",
                "stream=codec_type,codec_name", "-of", "default=noprint_wrappers=1",
                media.toString()).redirectErrorStream(true).start();
        String probeOutput = new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(probe.waitFor()).isZero();
        Files.deleteIfExists(media);
        assertThat(probeOutput).contains("codec_type=video", "codec_type=audio");
        VideoScript script = scriptRepository.findByTaskId(task.getId()).orElseThrow();
        assertThat(script.getFullText()).isNotBlank();
        assertThat(script.getScenes()).contains("durationSec");
    }

    private GenerationTask awaitTask(Long taskId) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
        while (Instant.now().isBefore(deadline)) {
            GenerationTask task = taskRepository.findById(taskId).orElseThrow();
            if ("SUCCESS".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
                return task;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("视频任务超时");
    }
}

