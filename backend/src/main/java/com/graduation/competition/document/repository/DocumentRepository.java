package com.graduation.competition.document.repository;

import com.graduation.competition.document.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    Page<DocumentEntity> findByCompetitionId(Long competitionId, Pageable pageable);

    Page<DocumentEntity> findByStatus(String status, Pageable pageable);

    List<DocumentEntity> findByDocumentTypeAndCompetitionId(String documentType, Long competitionId);
}

