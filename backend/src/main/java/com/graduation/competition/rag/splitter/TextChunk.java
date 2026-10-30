package com.graduation.competition.rag.splitter;

public record TextChunk(int chunkIndex, int pageNumber, String content, int tokenCount) {
}

