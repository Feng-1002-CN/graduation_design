package com.graduation.competition.agent.tool;

import com.graduation.competition.agent.dto.EvidenceItem;

import java.util.List;

public record GroupMatchResult(
        String groupName,
        String eligibility,
        String matchLevel,
        String reason,
        List<EvidenceItem> evidence) {
}

