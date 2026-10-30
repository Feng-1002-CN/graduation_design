package com.graduation.competition.task.repository;

import com.graduation.competition.task.entity.VideoScript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoScriptRepository extends JpaRepository<VideoScript, Long> {

    Optional<VideoScript> findByTaskId(Long taskId);
}

