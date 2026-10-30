package com.graduation.competition.plan.repository;

import com.graduation.competition.plan.entity.ProjectPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectPlanRepository extends JpaRepository<ProjectPlan, Long> {

    Optional<ProjectPlan> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
}

