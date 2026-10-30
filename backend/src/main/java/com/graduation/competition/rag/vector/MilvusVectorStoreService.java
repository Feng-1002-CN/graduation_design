package com.graduation.competition.rag.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Milvus v2 REST 向量库适配器。元数据使用动态字段保存，支持按竞赛、日期和作品类型过滤。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.vector.provider", havingValue = "milvus", matchIfMissing = true)
public class MilvusVectorStoreService implements VectorStoreService {

    private final RestClient client;
    private final String collection;
    private final int dimensions;
    private final AtomicBoolean initialized = new AtomicBoolean();

    public MilvusVectorStoreService(
            @Value("${app.milvus.base-url}") String baseUrl,
            @Value("${app.milvus.collection}") String collection,
            @Value("${app.embedding.dimensions:1024}") int dimensions,
            @Value("${app.milvus.token:}") String token) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (!token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }
        this.client = builder.build();
        this.collection = collection;
        this.dimensions = dimensions;
    }

    @Override
    public void upsert(List<VectorRecord> records) {
        ensureCollection(records.isEmpty() ? dimensions : records.get(0).vector().size());
        if (records.isEmpty()) {
            return;
        }
        List<Map<String, Object>> data = records.stream().map(record -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", record.id());
            item.put("vector", record.vector());
            item.put("document_id", record.documentId());
            item.put("competition_id", record.competitionId());
            item.put("source", record.source());
            item.put("source_date", record.sourceDate() == null ? null : record.sourceDate().toString());
            item.put("page_number", record.pageNumber());
            item.put("content", record.content());
            item.put("document_type", record.documentType());
            item.put("work_year", record.workYear());
            item.put("chunk_index", Integer.parseInt(record.id().substring(record.id().lastIndexOf('-') + 1)));
            return item;
        }).toList();
        execute("/v2/vectordb/entities/upsert", Map.of("collectionName", collection, "data", data));
    }

    @Override
    public List<SearchHit> search(List<Double> vector, SearchOptions options) {
        ensureCollection(vector.size());
        List<String> filters = new ArrayList<>();
        if (options.competitionId() != null) {
            filters.add("competition_id == " + options.competitionId());
        }
        if (options.sourceDateFrom() != null) {
            filters.add("source_date >= \"" + options.sourceDateFrom() + "\"");
        }
        if (options.documentType() != null && options.documentType().matches("[A-Z_]+")) {
            filters.add("document_type == \"" + options.documentType() + "\"");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("collectionName", collection);
        payload.put("data", List.of(vector));
        payload.put("annsField", "vector");
        payload.put("limit", options.topK());
        payload.put("outputFields", List.of("document_id", "chunk_index", "page_number",
                "content", "source", "source_date", "document_type", "work_year"));
        if (!filters.isEmpty()) {
            payload.put("filter", String.join(" and ", filters));
        }
        JsonNode response = execute("/v2/vectordb/entities/search", payload);
        List<SearchHit> hits = new ArrayList<>();
        for (JsonNode row : response.path("data")) {
            hits.add(toHit(row));
        }
        return hits;
    }

    @Override
    public void deleteByDocumentId(long documentId) {
        execute("/v2/vectordb/entities/delete", Map.of(
                "collectionName", collection,
                "filter", "document_id == " + documentId));
    }

    private SearchHit toHit(JsonNode row) {
        JsonNode entity = row.has("entity") ? row.path("entity") : row;
        String sourceDate = text(entity, "source_date");
        return new SearchHit(
                entity.path("document_id").asLong(),
                entity.path("chunk_index").asInt(),
                entity.has("page_number") ? entity.path("page_number").asInt() : null,
                text(entity, "content"),
                text(entity, "source"),
                sourceDate == null || sourceDate.isBlank() ? null : java.time.LocalDate.parse(sourceDate),
                row.path("distance").asDouble(row.path("score").asDouble()),
                text(entity, "document_type"),
                text(entity, "work_year"));
    }

    private String text(JsonNode row, String field) {
        JsonNode value = row.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private synchronized void ensureCollection(int vectorDimensions) {
        if (initialized.get()) {
            return;
        }
        try {
            client.post()
                    .uri("/v2/vectordb/collections/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "collectionName", collection,
                            "dimension", vectorDimensions,
                            "metricType", "COSINE",
                            "primaryFieldName", "id",
                            "vectorFieldName", "vector",
                            "idType", "VarChar"))
                    .retrieve()
                    .body(JsonNode.class);
            initialized.set(true);
            log.info("Milvus collection {} 已确认可用", collection);
        } catch (RuntimeException exception) {
            throw new BizException(ErrorCode.VECTOR_STORE_UNAVAILABLE,
                    "Milvus 不可用: " + exception.getMessage());
        }
    }

    private JsonNode execute(String path, Object payload) {
        try {
            return client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException exception) {
            throw new BizException(ErrorCode.VECTOR_STORE_UNAVAILABLE,
                    "Milvus 操作失败: " + exception.getMessage());
        }
    }
}

