package com.graduation.competition.task.repository;

import com.graduation.competition.task.entity.GenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerationTaskRepository extends JpaRepository<GenerationTask, Long> {

    List<GenerationTask> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}

