package com.graduation.competition.apply.repository;

import com.graduation.competition.apply.entity.ApplicationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationCheckRepository extends JpaRepository<ApplicationCheck, Long> {

    Optional<ApplicationCheck> findTopByCompetitionIdOrderByCreatedAtDesc(Long competitionId);
}

