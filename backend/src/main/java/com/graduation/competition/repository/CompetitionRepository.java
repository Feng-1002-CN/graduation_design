package com.graduation.competition.repository;

import com.graduation.competition.entity.Competition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompetitionRepository
        extends JpaRepository<Competition, Long>, JpaSpecificationExecutor<Competition> {

    @EntityGraph(attributePaths = {"groups", "rules", "rules.group"})
    @Query("select distinct c from Competition c where c.id = :id and c.deleted = false")
    Optional<Competition> findDetailsById(@Param("id") Long id);

    Optional<Competition> findByIdAndDeletedFalse(Long id);
}

