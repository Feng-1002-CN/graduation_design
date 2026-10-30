package com.graduation.competition.agent.tool;

import com.graduation.competition.agent.dto.EvidenceItem;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.entity.CompetitionGroup;
import com.graduation.competition.repository.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tool2：根据选题和组别资格进行可解释匹配。
 */
@Component
@RequiredArgsConstructor
public class MatchGroupTool {

    private final CompetitionRepository competitionRepository;

    @Tool(name = "matchGroup", description = "依据竞赛组别名称、参赛资格和选题方向匹配应报组别")
    public GroupMatchResult matchGroup(
            @ToolParam(description = "选题标题和技术设想") String topic,
            @ToolParam(description = "竞赛 ID，可为空", required = false) Long competitionId) {
        if (competitionId == null) {
            return new GroupMatchResult("未确定", "", "低",
                    "尚未指定竞赛，无法依据组别资格做可靠匹配。", List.of());
        }
        Competition competition = competitionRepository.findDetailsById(competitionId).orElse(null);
        if (competition == null || competition.getGroups().isEmpty()) {
            return new GroupMatchResult("未确定", "", "低",
                    "竞赛未维护组别资格，建议补充官方组别说明。", List.of());
        }
        CompetitionGroup group = competition.getGroups().stream()
                .filter(candidate -> !candidate.isDeleted())
                .max(Comparator.comparingDouble(candidate -> score(topic, candidate)))
                .orElse(null);
        if (group == null) {
            return new GroupMatchResult("未确定", "", "低", "没有可用组别。", List.of());
        }
        double score = score(topic, group);
        String level = score >= 0.28 ? "高" : score >= 0.1 ? "中" : "低";
        EvidenceItem evidence = new EvidenceItem(
                0, 0,
                "组别：" + group.getName() + "；资格：" + value(group.getEligibility()),
                competition.getSourceUrl(), null, competition.getSourceDate(), score, "GROUP");
        return new GroupMatchResult(group.getName(), group.getEligibility(), level,
                "该组别与选题方向存在文本匹配，需以官方资格条款和个人学籍核验为准。",
                List.of(evidence));
    }

    private double score(String topic, CompetitionGroup group) {
        String target = (value(group.getName()) + " " + value(group.getEligibility()))
                .toLowerCase(Locale.ROOT);
        int count = 0;
        for (int index = 0; index + 2 <= topic.length(); index += 2) {
            if (target.contains(topic.substring(index, index + 2))) {
                count++;
            }
        }
        return (double) count / Math.max(1, topic.length() / 2);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

