package com.graduation.competition.agent.tool;

import com.graduation.competition.agent.dto.EvidenceItem;
import com.graduation.competition.compare.dto.PastWorkCompareResult;
import com.graduation.competition.entity.Competition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool4：汇总证据并生成固定六维度的 Markdown 报告模板。
 */
@Component
public class GenerateReportTool {

    public String generate(
            String title,
            String description,
            Competition competition,
            List<EvidenceItem> ruleEvidence,
            GroupMatchResult group,
            List<PastWorkCompareResult> works) {
        String ruleSection = ruleEvidence.isEmpty()
                ? "未检索到可核验的有效规则片段，当前不能确认报名资格、材料要求或评分规范。"
                : ruleEvidence.stream().limit(4)
                .map(item -> "- " + compact(item.content(), 120) + " [E" + item.documentId() + "]")
                .collect(Collectors.joining("\n"));
        String similarSection = works.isEmpty()
                ? "- 未检索到可支持的往届相似作品，不能据此判断空白或重复。"
                : works.stream().limit(4)
                .map(work -> "- 《" + work.title() + "》：主题重合度" + work.topicOverlap()
                        + "，技术路线重合度" + work.techOverlap() + "。")
                .collect(Collectors.joining("\n"));
        String competitionName = competition == null ? "尚未匹配竞赛" : competition.getName();
        return """
                # 《%s》选题分析报告

                > 本报告依据已检索资料评价规则匹配、技术可行性、项目完整性、相似项目、选题风险与优化建议，不对评奖结果作任何预测。

                ## 规则匹配

                - 目标赛事：%s
                - 组别建议：%s（匹配度：%s）
                - 匹配依据：%s
                %s

                ## 技术可行性

                选题“%s”需要先验证数据可得性、算法基线、推理算力与演示环境。建议采用“可替换模型 + 小样本验证 + 可回退方案”，优先打通数据采集、识别、结果展示与人工复核闭环。当前资料未提供数据规模或硬件条件，相关指标必须通过原型测试确认。

                ## 项目完整性

                作品至少应形成数据准备、核心算法、业务服务、交互界面、测试评估和演示材料六个部分。演示阶段需准备标准输入、典型失败案例和人工复核流程，确保系统能够独立展示，而不是只展示离线指标。

                ## 相似项目

                %s

                ## 选题风险

                - 资料风险：%s
                - 技术风险：数据质量、泛化能力或推理时延可能无法支撑真实演示。
                - 周期风险：若模型训练与业务开发并行，需尽早冻结最小可用功能。
                - 规则风险：报名资格、材料清单与截止时间应以官方通知再次核验。

                ## 优化建议

                1. 先用代表性样本建立可量化基线和验收指标，再扩展复杂功能。
                2. 在识别结果中加入置信度、证据或人工复核入口，降低误判影响。
                3. 围绕真实应用场景形成差异化，明确数据、算法或交互上的可展示创新点。
                4. 将规则要求逐项映射到报名材料和演示流程，避免后期返工。

                ## 证据链

                %s
                """.formatted(
                title,
                competitionName,
                group.groupName(),
                group.matchLevel(),
                group.reason(),
                ruleSection,
                title,
                similarSection,
                ruleEvidence.isEmpty() ? "知识库规则证据不足。" : "已取得 " + ruleEvidence.size() + " 条规则证据。",
                evidenceList(ruleEvidence));
    }

    private String evidenceList(List<EvidenceItem> evidence) {
        if (evidence.isEmpty()) {
            return "- 未检索到可支持结论的资料。";
        }
        return evidence.stream().map(item -> "- %s；第 %s 页；来源日期 %s；相关度 %.3f"
                        .formatted(item.source(), item.pageNumber() == null ? "—" : item.pageNumber(),
                                item.sourceDate() == null ? "未提供" : item.sourceDate(), item.score()))
                .distinct().collect(Collectors.joining("\n"));
    }

    private String compact(String text, int length) {
        String value = text.replace('\n', ' ').trim();
        return value.length() <= length ? value : value.substring(0, length) + "…";
    }
}

