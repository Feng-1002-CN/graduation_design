package com.graduation.competition;

import com.graduation.competition.agent.dto.AgentEvent;
import com.graduation.competition.agent.dto.AnalyzeRequest;
import com.graduation.competition.agent.service.AnalysisAgentService;
import com.graduation.competition.project.entity.AnalysisReport;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.AnalysisReportRepository;
import com.graduation.competition.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class AnalysisAgentTest {

    @Autowired
    private AnalysisAgentService analysisAgentService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AnalysisReportRepository reportRepository;

    @Test
    void executesFourStepsAndPersistsSixDimensionReport() {
        List<AgentEvent> events = new ArrayList<>();
        analysisAgentService.execute(new AnalyzeRequest(
                null,
                "基于计算机视觉的中药材识别系统",
                "使用图像识别与机器学习实现中药材分类，并支持人工复核。",
                null
        ), events::add);

        assertThat(events.stream().filter(event -> "step_start".equals(event.type())).count()).isEqualTo(4);
        assertThat(events.stream().filter(event -> "step_end".equals(event.type())).count()).isEqualTo(4);
        assertThat(events).anyMatch(event -> "done".equals(event.type()));
        String report = events.stream().filter(event -> "delta".equals(event.type()))
                .map(AgentEvent::content).reduce("", String::concat);
        assertThat(report).contains("## 规则匹配", "## 技术可行性", "## 项目完整性",
                "## 相似项目", "## 选题风险", "## 优化建议");
        assertThat(report).doesNotContain("获奖概率", "获奖率");

        Project project = projectRepository.findFirstByTitleOrderByIdDesc(
                "基于计算机视觉的中药材识别系统").orElseThrow();
        AnalysisReport saved = reportRepository.findTopByProjectIdOrderByCreatedAtDesc(project.getId())
                .orElseThrow();
        assertThat(saved.getContent()).isEqualTo(report);
        assertThat(events).anyMatch(event -> "done".equals(event.type()) && event.reportId() != null);
    }
}

