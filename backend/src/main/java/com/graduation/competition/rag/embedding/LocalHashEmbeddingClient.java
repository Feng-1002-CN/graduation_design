package com.graduation.competition.rag.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 仅用于本地演示与自动化测试的确定性哈希向量，不替代生产环境 BGE/M3E。
 */
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "local")
public class LocalHashEmbeddingClient implements EmbeddingClient {

    private final int dimensions;

    public LocalHashEmbeddingClient(@Value("${app.embedding.dimensions:1024}") int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        return texts.stream().map(this::embedOne).toList();
    }

    private List<Double> embedOne(String text) {
        List<Double> vector = new ArrayList<>(dimensions);
        for (int index = 0; index < dimensions; index++) {
            vector.add(0.0);
        }
        for (int index = 0; index < text.length(); index++) {
            int hash = text.substring(index, Math.min(text.length(), index + 2))
                    .getBytes(StandardCharsets.UTF_8).length * 31 + text.charAt(index);
            int position = Math.floorMod(hash, dimensions);
            vector.set(position, vector.get(position) + (hash % 2 == 0 ? 1.0 : -1.0));
        }
        double norm = Math.sqrt(vector.stream().mapToDouble(value -> value * value).sum());
        if (norm == 0) {
            return vector;
        }
        return vector.stream().map(value -> value / norm).toList();
    }
}

