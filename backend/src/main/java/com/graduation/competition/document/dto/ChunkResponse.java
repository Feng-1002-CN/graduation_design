package com.graduation.competition.document.dto;

public record ChunkResponse(
        Long id,
        int chunkIndex,
        String content,
        Integer pageNumber,
        Integer tokenCount,
        String vectorId) {
}

