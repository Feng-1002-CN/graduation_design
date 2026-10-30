package com.graduation.competition.agent.tool;

import com.graduation.competition.agent.dto.EvidenceItem;
import com.graduation.competition.compare.dto.PastWorkCompareRequest;
import com.graduation.competition.compare.dto.PastWorkCompareResult;
import com.graduation.competition.compare.service.PastWorkCompareService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool3：复用往届作品检索，仅返回有相似度与来源的结果。
 */
@Component
@RequiredArgsConstructor
public class SearchPastWorksTool {

    private final PastWorkCompareService compareService;

    @Tool(name = "searchPastWorks", description = "检索与选题相似且可溯源的往届作品")
    public List<PastWorkCompareResult> searchPastWorks(
            @ToolParam(description = "选题标题和技术设想") String topic,
            @ToolParam(description = "竞赛 ID，可为空", required = false) Long competitionId) {
        return compareService.compare(new PastWorkCompareRequest(topic, competitionId, 5));
    }

    public List<EvidenceItem> toEvidence(List<PastWorkCompareResult> works) {
        return works.stream().map(work -> new EvidenceItem(
                work.documentId(), 0, work.overlapDesc(), work.source(),
                null, work.sourceDate(), work.score(), "PAST_WORK")).toList();
    }
}

