package com.graduation.competition.rag.embedding;

import java.util.List;

public interface EmbeddingClient {

    List<List<Double>> embed(List<String> texts);
}

