package com.graduation.competition.agent.tool;

import com.graduation.competition.agent.dto.EvidenceItem;
import com.graduation.competition.entity.Competition;

import java.util.List;

public record RuleEvidenceBundle(
        Competition competition,
        List<EvidenceItem> evidence,
        String autoMatchReason) {
}

