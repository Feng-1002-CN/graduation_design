package com.graduation.competition.plan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.ai.AiGateway;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.plan.dto.PlanDTO;
import com.graduation.competition.plan.entity.ProjectPlan;
import com.graduation.competition.plan.repository.ProjectPlanRepository;
import com.graduation.competition.project.entity.AnalysisReport;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.AnalysisReportRepository;
import com.graduation.competition.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目方案生成与维护。结构化 modules 字段可直接供 PPT/视频分镜消费。
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private static final String SYSTEM_PROMPT = """
            你是学科竞赛项目方案助手。只输出严格 JSON，不使用 Markdown 代码块，不添加解释。
            JSON 字段：title, background, requirements,
            modules:[{name,description}], techRoute, innovation, schedule。
            不得编造已上传资料中没有的事实，不得预测获奖概率或评奖结果。
            """;

    private final ProjectRepository projectRepository;
    private final AnalysisReportRepository reportRepository;
    private final ProjectPlanRepository planRepository;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.provider:openai}")
    private String aiProvider;

    @Transactional
    public PlanDTO generate(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在"));
        String report = reportRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .map(AnalysisReport::getContent).orElse("尚无分析报告。");
        PlanDTO generated = "extractive".equalsIgnoreCase(aiProvider)
                ? deterministic(project)
                : parseAiResult(aiGateway.complete(SYSTEM_PROMPT,
                "项目标题：" + project.getTitle()
                        + "\n项目描述：" + value(project.getDescription())
                        + "\n分析报告：" + report), project);
        ProjectPlan entity = planRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseGet(ProjectPlan::new);
        entity.setProjectId(projectId);
        apply(entity, generated, projectId);
        return toDto(planRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public PlanDTO getByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在");
        }
        return planRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .map(this::toDto)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目方案不存在"));
    }

    @Transactional
    public PlanDTO update(Long id, PlanDTO dto) {
        ProjectPlan entity = planRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目方案不存在"));
        apply(entity, dto, entity.getProjectId());
        return toDto(planRepository.save(entity));
    }

    private PlanDTO parseAiResult(String content, Project project) {
        try {
            String json = content.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalStateException("未返回 JSON 对象");
            }
            PlanDTO parsed = objectMapper.readValue(json.substring(start, end + 1), PlanDTO.class);
            validate(parsed, project);
            return parsed;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.MODEL_UNAVAILABLE,
                    "方案 JSON 解析或校验失败: " + exception.getMessage());
        }
    }

    private void validate(PlanDTO plan, Project project) {
        if (plan.title() == null || plan.background() == null || plan.requirements() == null
                || plan.modules() == null || plan.modules().isEmpty()
                || plan.techRoute() == null || plan.innovation() == null || plan.schedule() == null) {
            throw new IllegalArgumentException("方案缺少必填字段");
        }
    }

    private PlanDTO deterministic(Project project) {
        String title = project.getTitle();
        String description = value(project.getDescription());
        return new PlanDTO(
                null,
                project.getId(),
                title + "项目方案",
                "面向计算机类学科竞赛，围绕“" + title + "”构建可演示、可评估、可维护的软件作品。项目背景来自用户选题描述，后续应结合有效竞赛规则核验。",
                "核心需求：完成数据/素材管理、核心算法或业务处理、结果展示、质量验证与演示输出。用户描述："
                        + description,
                List.of(
                        new PlanDTO.ModuleDTO("数据与素材管理", "管理输入数据、文件、来源信息与处理状态。"),
                        new PlanDTO.ModuleDTO("核心处理模块", "实现选题对应的算法、分析或生成流程，并输出可解释结果。"),
                        new PlanDTO.ModuleDTO("业务服务", "提供统一接口、任务状态、错误处理与结果持久化。"),
                        new PlanDTO.ModuleDTO("交互与展示", "提供任务配置、进度、结果核验及内容下载。"),
                        new PlanDTO.ModuleDTO("测试评估", "建立基线数据、功能测试、异常场景与效果指标。")),
                "Spring Boot 3 + Vue 3 + PostgreSQL；按选题接入可替换算法/模型；文件使用 MinIO；长任务采用异步执行。",
                "以可溯源证据链、人工复核机制和场景化交互形成差异化创新，具体创新点需通过原型实验验证。",
                "第 1-2 周：需求与规则核验、数据准备；第 3-5 周：核心模块与接口；第 6-7 周：前端联调与测试；第 8 周：演示材料、回归验收。",
                null,
                null);
    }

    private void apply(ProjectPlan entity, PlanDTO dto, Long projectId) {
        try {
            entity.setProjectId(projectId);
            entity.setTitle(dto.title());
            entity.setBackground(dto.background());
            entity.setRequirements(dto.requirements());
            entity.setModules(objectMapper.writeValueAsString(dto.modules()));
            entity.setTechRoute(dto.techRoute());
            entity.setInnovation(dto.innovation());
            entity.setSchedule(dto.schedule());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "方案模块清单格式错误");
        }
    }

    private PlanDTO toDto(ProjectPlan plan) {
        try {
            List<PlanDTO.ModuleDTO> modules = objectMapper.readValue(
                    plan.getModules() == null ? "[]" : plan.getModules(), new TypeReference<>() { });
            return new PlanDTO(plan.getId(), plan.getProjectId(), plan.getTitle(),
                    plan.getBackground(), plan.getRequirements(), modules, plan.getTechRoute(),
                    plan.getInnovation(), plan.getSchedule(), plan.getCreatedAt(), plan.getUpdatedAt());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "方案模块数据损坏");
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

