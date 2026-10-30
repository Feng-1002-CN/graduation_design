package com.graduation.competition;

import com.graduation.competition.plan.service.PlanService;
import com.graduation.competition.project.entity.AgentTrace;
import com.graduation.competition.project.entity.AnalysisReport;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.AgentTraceRepository;
import com.graduation.competition.project.repository.AnalysisReportRepository;
import com.graduation.competition.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WorkbenchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PlanService planService;

    @Autowired
    private AnalysisReportRepository reportRepository;

    @Autowired
    private AgentTraceRepository traceRepository;

    @Test
    void aggregatesProjectDataAndTimeline() throws Exception {
        Project project = new Project();
        project.setTitle("工作台聚合测试项目");
        project.setDescription("用于验证模块聚合");
        project = projectRepository.save(project);
        planService.generate(project.getId());

        AnalysisReport report = new AnalysisReport();
        report.setProjectId(project.getId());
        report.setContent("# 分析报告\n\n## 规则匹配\n已匹配有效规则。");
        report.setRefs("[]");
        reportRepository.save(report);

        AgentTrace trace = new AgentTrace();
        trace.setProjectId(project.getId());
        trace.setStep(1);
        trace.setToolName("searchRules");
        trace.setInput("测试输入");
        trace.setOutput("命中 2 条规则");
        trace.setStartedAt(Instant.now().minusSeconds(2));
        trace.setEndedAt(Instant.now().minusSeconds(1));
        traceRepository.save(trace);

        mockMvc.perform(get("/api/v1/workbench/projects/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project.title").value("工作台聚合测试项目"))
                .andExpect(jsonPath("$.data.latestReport.summary").value(org.hamcrest.Matchers.containsString("规则匹配")))
                .andExpect(jsonPath("$.data.plan.title").isNotEmpty())
                .andExpect(jsonPath("$.data.tasks").isArray())
                .andExpect(jsonPath("$.data.traces[0].toolName").value("searchRules"))
                .andExpect(jsonPath("$.data.timeline").isArray());

        mockMvc.perform(get("/api/v1/workbench/projects").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.id == " + project.getId() + ")]").isNotEmpty());
    }
}

