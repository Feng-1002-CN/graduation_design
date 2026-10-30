package com.graduation.competition.apply.dto;

import java.time.LocalDate;

public record CheckItemResult(
        String item,
        String status,
        String fileName,
        String advice,
        String source,
        LocalDate sourceDate) {
}

