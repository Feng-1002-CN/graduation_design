package com.graduation.competition.repository;

import com.graduation.competition.entity.CompetitionRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompetitionRuleRepository extends JpaRepository<CompetitionRule, Long> {

    Optional<CompetitionRule> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"competition", "group"})
    @Query("""
            select r from CompetitionRule r
            where r.competition.id = :competitionId
              and r.deleted = false
              and r.competition.deleted = false
              and (:activeOnly = false or (
                    r.status = 'ACTIVE'
                    and (r.competition.contestEnd is null or r.competition.contestEnd >= :today)
              ))
            order by r.createdAt desc
            """)
    List<CompetitionRule> findRules(
            @Param("competitionId") Long competitionId,
            @Param("activeOnly") boolean activeOnly,
            @Param("today") LocalDate today);
}

