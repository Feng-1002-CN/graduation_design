package com.graduation.competition;

import com.graduation.competition.common.BizException;
import com.graduation.competition.plan.service.PlanService;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.ProjectRepository;
import com.graduation.competition.task.entity.GenerationTask;
import com.graduation.competition.task.repository.GenerationTaskRepository;
import com.graduation.competition.task.service.PptGenerateTask;
import com.graduation.competition.task.service.TaskService;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class PptGenerationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PlanService planService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private PptGenerateTask generateTask;

    @Autowired
    private GenerationTaskRepository taskRepository;

    @Autowired
    private com.graduation.competition.storage.ObjectStorageService storageService;

    @Test
    void generatesEditablePptxAndCompletesTask() throws Exception {
        Project project = new Project();
        project.setTitle("中药材智能识别项目");
        project.setDescription("图像识别与人工复核");
        project = projectRepository.save(project);
        planService.generate(project.getId());
        GenerationTask task = taskService.createPptTask(project.getId());
        generateTask.execute(task.getId(), "default");

        GenerationTask completed = awaitTask(task.getId());
        assertThat(completed.getStatus()).isEqualTo("SUCCESS");
        assertThat(completed.getProgress()).isEqualTo(100);
        assertThat(completed.getFileUrl()).isNotBlank();

        try (InputStream input = storageService.get(completed.getFileUrl());
             XMLSlideShow show = new XMLSlideShow(input)) {
            assertThat(show.getSlides()).hasSize(5);
            String allText = allText(show);
            assertThat(allText).contains("中药材智能识别项目");
            assertThat(allText).doesNotContain("{{title}}", "{{modules}}", "{{techRoute}}");
        }
    }

    private GenerationTask awaitTask(Long taskId) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(8));
        while (Instant.now().isBefore(deadline)) {
            GenerationTask task = taskRepository.findById(taskId).orElseThrow();
            if ("SUCCESS".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
                return task;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("PPT 任务超时");
    }

    private String allText(XMLSlideShow show) {
        StringBuilder builder = new StringBuilder();
        for (XSLFSlide slide : show.getSlides()) {
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape text) {
                    builder.append(text.getText()).append('\n');
                }
            }
        }
        return builder.toString();
    }
}

