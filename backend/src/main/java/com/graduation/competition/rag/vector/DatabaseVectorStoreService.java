package com.graduation.competition.rag.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.document.entity.DocumentChunk;
import com.graduation.competition.document.entity.DocumentEntity;
import com.graduation.competition.document.repository.DocumentChunkRepository;
import com.graduation.competition.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据库降级向量存储，用于本地开发与测试；检索算法与 Milvus 保持相同余弦语义。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.vector.provider", havingValue = "database")
public class DatabaseVectorStoreService implements VectorStoreService {

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void upsert(List<VectorRecord> records) {
        // 切片在解析流程中已持久化；本地模式无需重复写入。
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHit> search(List<Double> vector, SearchOptions options) {
        List<DocumentEntity> documents = documentRepository.findAll();
        Map<Long, DocumentEntity> documentMap = documents.stream()
                .collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));
        List<DocumentChunk> chunks = chunkRepository.findAll();
        List<SearchHit> hits = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            DocumentEntity document = documentMap.get(chunk.getDocumentId());
            if (document == null || chunk.getEmbedding() == null
                    || !"READY".equals(document.getStatus())) {
                continue;
            }
            if (options.competitionId() != null
                    && !options.competitionId().equals(document.getCompetitionId())) {
                continue;
            }
            if (options.sourceDateFrom() != null && document.getSourceDate() != null
                    && document.getSourceDate().isBefore(options.sourceDateFrom())) {
                continue;
            }
            if (options.documentType() != null
                    && !options.documentType().equalsIgnoreCase(document.getDocumentType())) {
                continue;
            }
            try {
                List<Double> stored = objectMapper.readValue(
                        chunk.getEmbedding(), new TypeReference<>() { });
                hits.add(new SearchHit(
                        document.getId(),
                        chunk.getChunkIndex(),
                        chunk.getPageNumber(),
                        chunk.getContent(),
                        document.getSourceUrl() != null ? document.getSourceUrl() : document.getFileUrl(),
                        document.getSourceDate(),
                        cosine(vector, stored),
                        document.getDocumentType(),
                        document.getWorkYear()));
            } catch (Exception ignored) {
                // 损坏的单个向量不影响其余切片检索。
            }
        }
        return hits.stream()
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(options.topK())
                .toList();
    }

    @Override
    @Transactional
    public void deleteByDocumentId(long documentId) {
        chunkRepository.deleteByDocumentId(documentId);
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.size() != right.size()) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        return leftNorm == 0 || rightNorm == 0 ? 0
                : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}

