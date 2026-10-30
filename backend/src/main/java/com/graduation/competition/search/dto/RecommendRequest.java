package com.graduation.competition.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record RecommendRequest(
        String grade,
        String direction,
        List<String> keywords,
        @Min(1) @Max(50) Integer limit) {

    public int resolvedLimit() {
        return limit == null ? 10 : limit;
    }

    public String queryText() {
        return String.join(" ",
                grade == null ? "" : grade,
                direction == null ? "" : direction,
                keywords == null ? "" : String.join(" ", keywords)).trim();
    }
}

