package com.graduation.competition.document.dto;

import com.graduation.competition.document.entity.DocumentEntity;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        Long competitionId,
        String title,
        String fileType,
        String fileUrl,
        String originalFilename,
        String sourceUrl,
        LocalDate sourceDate,
        String status,
        int chunkCount,
        String errorMsg,
        String documentType,
        String workYear,
        Instant createdAt) {

    public static DocumentResponse from(DocumentEntity document) {
        return new DocumentResponse(
                document.getId(), document.getCompetitionId(), document.getTitle(),
                document.getFileType(), document.getFileUrl(), document.getOriginalFilename(),
                document.getSourceUrl(), document.getSourceDate(), document.getStatus(),
                document.getChunkCount(), document.getErrorMsg(), document.getDocumentType(),
                document.getWorkYear(), document.getCreatedAt());
    }
}

