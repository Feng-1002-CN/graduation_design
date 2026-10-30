package com.graduation.competition.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat Completions 网关。系统提示中统一注入研究诚信与证据约束。
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai", matchIfMissing = true)
public class RemoteAiGateway implements AiGateway {

    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final double temperature;

    public RemoteAiGateway(
            @Qualifier("aiRestClient") RestClient client,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.temperature:0.2}") double temperature) {
        this.client = client;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) {
            throw new BizException(ErrorCode.MODEL_UNAVAILABLE, "未配置 AI_API_KEY");
        }
        try {
            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "temperature", temperature,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt))))
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null ? ""
                    : response.path("choices").path(0).path("message").path("content").asText();
            if (content.isBlank()) {
                throw new IllegalStateException("模型未返回内容");
            }
            return content;
        } catch (RuntimeException exception) {
            throw new BizException(ErrorCode.MODEL_UNAVAILABLE,
                    "AI 模型调用失败: " + exception.getMessage());
        }
    }
}

