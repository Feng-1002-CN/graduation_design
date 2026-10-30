package com.graduation.competition.apply.dto;

import java.time.LocalDate;

public record RequiredItem(
        String item,
        boolean required,
        String source,
        LocalDate sourceDate) {
}

