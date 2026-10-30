package com.graduation.competition.apply.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyFile(
        @NotBlank String name,
        String type,
        @NotBlank String url) {
}

