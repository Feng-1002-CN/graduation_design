package com.graduation.competition.project.repository;

import com.graduation.competition.project.entity.AgentTrace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTraceRepository extends JpaRepository<AgentTrace, Long> {

    List<AgentTrace> findByProjectIdOrderByStepAsc(Long projectId);
}

