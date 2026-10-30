package com.graduation.competition.rule.repository;

import com.graduation.competition.rule.entity.QaLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaLogRepository extends JpaRepository<QaLog, Long> {
}

