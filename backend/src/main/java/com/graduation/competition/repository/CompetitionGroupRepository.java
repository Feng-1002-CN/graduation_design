package com.graduation.competition.repository;

import com.graduation.competition.entity.CompetitionGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionGroupRepository extends JpaRepository<CompetitionGroup, Long> {

    @EntityGraph(attributePaths = "competition")
    Optional<CompetitionGroup> findByIdAndDeletedFalse(Long id);
}

