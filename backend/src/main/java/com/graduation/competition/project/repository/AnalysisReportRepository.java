package com.graduation.competition.project.repository;

import com.graduation.competition.project.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    Optional<AnalysisReport> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
}

