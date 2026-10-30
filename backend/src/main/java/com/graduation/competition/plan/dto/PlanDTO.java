package com.graduation.competition.plan.dto;

import java.time.Instant;
import java.util.List;

public record PlanDTO(
        Long id,
        Long projectId,
        String title,
        String background,
        String requirements,
        List<ModuleDTO> modules,
        String techRoute,
        String innovation,
        String schedule,
        Instant createdAt,
        Instant updatedAt) {

    public record ModuleDTO(String name, String description) {
    }

    public PlanDTO withIdentity(Long id, Long projectId, Instant createdAt, Instant updatedAt) {
        return new PlanDTO(id, projectId, title, background, requirements, modules,
                techRoute, innovation, schedule, createdAt, updatedAt);
    }
}

