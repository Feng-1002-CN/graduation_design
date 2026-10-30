package com.graduation.competition.rag.embedding;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BGE/M3E OpenAI 兼容 Embedding 客户端，包含批量、超时和指数退避重试。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "remote", matchIfMissing = true)
public class RemoteEmbeddingClient implements EmbeddingClient {

    private final RestClient client;
    private final String path;
    private final String apiKey;
    private final String model;

    public RemoteEmbeddingClient(
            @Qualifier("embeddingRestClient") RestClient client,
            @Value("${app.embedding.path}") String path,
            @Value("${app.embedding.api-key:}") String apiKey,
            @Value("${app.embedding.model}") String model) {
        this.client = client;
        this.path = path;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        List<List<Double>> result = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += 16) {
            result.addAll(embedBatch(texts.subList(start, Math.min(texts.size(), start + 16))));
        }
        return result;
    }

    private List<List<Double>> embedBatch(List<String> texts) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                RestClient.RequestBodySpec request = client.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON);
                if (!apiKey.isBlank()) {
                    request.header("Authorization", "Bearer " + apiKey);
                }
                EmbeddingResponse response = request
                        .body(Map.of("model", model, "input", texts))
                        .retrieve()
                        .body(EmbeddingResponse.class);
                if (response == null || response.data() == null || response.data().size() != texts.size()) {
                    throw new IllegalStateException("Embedding 服务返回数据不完整");
                }
                return response.data().stream()
                        .sorted((left, right) -> Integer.compare(left.index(), right.index()))
                        .map(EmbeddingItem::embedding)
                        .toList();
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn("Embedding 请求失败，第 {} 次重试: {}", attempt, exception.getMessage());
                sleep(250L * attempt);
            }
        }
        throw new BizException(ErrorCode.MODEL_UNAVAILABLE,
                "Embedding 服务不可用: " + (lastFailure == null ? "unknown" : lastFailure.getMessage()));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    record EmbeddingResponse(List<EmbeddingItem> data) {
    }

    record EmbeddingItem(int index, List<Double> embedding) {
    }
}

