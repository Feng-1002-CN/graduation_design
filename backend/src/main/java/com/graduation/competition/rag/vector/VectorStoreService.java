package com.graduation.competition.rag.vector;

import java.util.List;

public interface VectorStoreService {

    void upsert(List<VectorRecord> records);

    List<SearchHit> search(List<Double> vector, SearchOptions options);

    void deleteByDocumentId(long documentId);
}

