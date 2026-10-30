package com.graduation.competition.gen.video;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.ai.AiGateway;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.plan.dto.PlanDTO;
import com.graduation.competition.plan.service.PlanService;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分镜脚本生成器。只生成讲解文本、时长与素材映射，不生成视频画面。
 */
@Component
@RequiredArgsConstructor
public class ScriptGenerator {

    private static final String SYSTEM_PROMPT = """
            为竞赛项目演示视频生成分镜脚本。严格输出 JSON 数组，每项包含
            text（中文旁白）、durationSec（3-12 秒整数）、imageUrl（可为空）。
            只依据项目方案撰写，禁止虚构数据，禁止预测获奖。
            """;

    private final ProjectRepository projectRepository;
    private final PlanService planService;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.provider:openai}")
    private String aiProvider;

    public List<VideoScene> generate(Long projectId, List<String> images) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在"));
        PlanDTO plan = planService.getByProjectId(projectId);
        if (!"extractive".equalsIgnoreCase(aiProvider)) {
            try {
                String response = aiGateway.complete(SYSTEM_PROMPT,
                        "项目：" + project.getTitle() + "\n方案：" + objectMapper.writeValueAsString(plan));
                List<VideoScene> scenes = parse(response);
                return assignImages(scenes, images);
            } catch (Exception ignored) {
                // 远端模型输出不合格时使用基于结构化方案的保守本地分镜。
            }
        }
        return localScenes(plan, images);
    }

    private List<VideoScene> parse(String content) throws Exception {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("未返回 JSON 数组");
        }
        List<VideoScene> scenes = objectMapper.readValue(
                content.substring(start, end + 1), new TypeReference<>() { });
        if (scenes.isEmpty() || scenes.stream().anyMatch(scene ->
                scene.text() == null || scene.text().isBlank()
                        || scene.durationSec() < 3 || scene.durationSec() > 12)) {
            throw new IllegalArgumentException("分镜字段校验失败");
        }
        return scenes;
    }

    private List<VideoScene> localScenes(PlanDTO plan, List<String> images) {
        List<String> texts = List.of(
                "大家好，这是" + plan.title() + "。作品聚焦真实需求，强调可演示的完整闭环。",
                "项目背景与需求如下：" + compact(plan.background(), 90),
                "核心功能包括：" + plan.modules().stream().map(PlanDTO.ModuleDTO::name)
                        .limit(4).collect(java.util.stream.Collectors.joining("、")) + "。",
                "技术路线采用 " + compact(plan.techRoute(), 80) + "。创新点在于" + compact(plan.innovation(), 60),
                "后续将按计划完成测试、演示与材料整理：" + compact(plan.schedule(), 80));
        List<VideoScene> scenes = new ArrayList<>();
        for (int index = 0; index < texts.size(); index++) {
            scenes.add(new VideoScene(texts.get(index), 5,
                    images == null || images.isEmpty() ? null : images.get(index % images.size())));
        }
        return scenes;
    }

    private List<VideoScene> assignImages(List<VideoScene> scenes, List<String> images) {
        if (images == null || images.isEmpty()) {
            return scenes;
        }
        return java.util.stream.IntStream.range(0, scenes.size())
                .mapToObj(index -> new VideoScene(
                        scenes.get(index).text(),
                        scenes.get(index).durationSec(),
                        scenes.get(index).imageUrl() != null
                                ? scenes.get(index).imageUrl()
                                : images.get(index % images.size())))
                .toList();
    }

    private String compact(String value, int length) {
        String result = value == null ? "" : value.replace('\n', ' ');
        return result.length() <= length ? result : result.substring(0, length) + "…";
    }
}

