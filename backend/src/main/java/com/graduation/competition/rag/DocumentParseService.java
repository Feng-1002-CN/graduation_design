package com.graduation.competition.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.document.entity.DocumentChunk;
import com.graduation.competition.document.entity.DocumentEntity;
import com.graduation.competition.document.repository.DocumentChunkRepository;
import com.graduation.competition.document.repository.DocumentRepository;
import com.graduation.competition.rag.embedding.EmbeddingClient;
import com.graduation.competition.rag.parser.DocumentParserFactory;
import com.graduation.competition.rag.parser.ParsedPage;
import com.graduation.competition.rag.splitter.TextChunk;
import com.graduation.competition.rag.splitter.TextChunkSplitter;
import com.graduation.competition.rag.vector.VectorRecord;
import com.graduation.competition.rag.vector.VectorStoreService;
import com.graduation.competition.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 异步编排解析、切分、向量化、写入与状态更新。任一步失败均保留明确错误原因。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParseService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ObjectStorageService storageService;
    private final DocumentParserFactory parserFactory;
    private final TextChunkSplitter splitter;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final ObjectMapper objectMapper;

    @Async("applicationTaskExecutor")
    public void process(Long documentId) {
        DocumentEntity document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return;
        }
        document.setStatus("PROCESSING");
        document.setErrorMsg(null);
        documentRepository.save(document);
        try {
            byte[] bytes;
            try (InputStream input = storageService.get(document.getFileUrl())) {
                bytes = input.readAllBytes();
            }
            List<ParsedPage> pages = parserFactory.get(document.getFileType()).parse(bytes);
            List<TextChunk> pieces = splitter.split(pages);
            if (pieces.isEmpty()) {
                throw new BizException(ErrorCode.INVALID_FILE, "文档未提取到可检索文本");
            }
            persistChunks(document, pieces);
            document.setChunkCount(pieces.size());
            document.setStatus("READY");
            documentRepository.save(document);
            log.info("文档 {} 处理完成，共 {} 个切片", documentId, pieces.size());
        } catch (Exception exception) {
            log.error("文档 {} 处理失败", documentId, exception);
            document.setStatus("FAILED");
            document.setErrorMsg(limit(exception.getMessage(), 2000));
            documentRepository.save(document);
        }
    }

    @Transactional
    protected void persistChunks(DocumentEntity document, List<TextChunk> pieces) throws Exception {
        chunkRepository.deleteByDocumentId(document.getId());
        List<DocumentChunk> chunks = new ArrayList<>();
        for (TextChunk piece : pieces) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(piece.chunkIndex());
            chunk.setContent(piece.content());
            chunk.setPageNumber(piece.pageNumber());
            chunk.setTokenCount(piece.tokenCount());
            chunks.add(chunk);
        }
        List<List<Double>> vectors = embeddingClient.embed(
                pieces.stream().map(TextChunk::content).toList());
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunk chunk = chunks.get(index);
            chunk.setEmbedding(objectMapper.writeValueAsString(vectors.get(index)));
            chunk.setVectorId(document.getId() + "-" + chunk.getChunkIndex());
        }
        chunkRepository.saveAll(chunks);
        chunkRepository.flush();

        List<VectorRecord> records = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunk chunk = chunks.get(index);
            records.add(new VectorRecord(
                    chunk.getVectorId(),
                    document.getId(),
                    document.getCompetitionId(),
                    document.getSourceUrl() == null ? document.getFileUrl() : document.getSourceUrl(),
                    document.getSourceDate(),
                    chunk.getPageNumber(),
                    chunk.getContent(),
                    vectors.get(index),
                    document.getDocumentType(),
                    document.getWorkYear()));
        }
        vectorStoreService.upsert(records);
    }

    private String limit(String message, int length) {
        if (message == null) {
            return "未知错误";
        }
        return message.length() <= length ? message : message.substring(0, length);
    }
}

