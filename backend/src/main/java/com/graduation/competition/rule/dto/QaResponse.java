package com.graduation.competition.rule.dto;

import java.util.List;

public record QaResponse(
        String answer,
        List<Reference> references,
        boolean fallback) {
}

