package com.graduation.competition.document.service.impl;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.common.PageResult;
import com.graduation.competition.document.dto.ChunkResponse;
import com.graduation.competition.document.dto.DocumentResponse;
import com.graduation.competition.document.dto.DocumentUploadResult;
import com.graduation.competition.document.dto.SearchRequest;
import com.graduation.competition.document.entity.DocumentChunk;
import com.graduation.competition.document.entity.DocumentEntity;
import com.graduation.competition.document.repository.DocumentChunkRepository;
import com.graduation.competition.document.repository.DocumentRepository;
import com.graduation.competition.document.service.DocumentService;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.rag.DocumentParseService;
import com.graduation.competition.rag.embedding.EmbeddingClient;
import com.graduation.competition.rag.vector.SearchHit;
import com.graduation.competition.rag.vector.SearchOptions;
import com.graduation.competition.rag.vector.VectorStoreService;
import com.graduation.competition.repository.CompetitionRepository;
import com.graduation.competition.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final CompetitionRepository competitionRepository;
    private final ObjectStorageService storageService;
    private final DocumentParseService parseService;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;

    @Override
    @Transactional
    public DocumentUploadResult upload(
            MultipartFile file,
            Long competitionId,
            String title,
            String sourceUrl,
            LocalDate sourceDate,
            String documentType,
            String workYear) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_FILE, "请选择需要上传的 PDF 或 DOCX 文件");
        }
        String filename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String extension = extension(filename);
        if (!List.of("pdf", "docx").contains(extension)) {
            throw new BizException(ErrorCode.INVALID_FILE, "仅支持 PDF 与 DOCX 文件");
        }
        if (competitionId != null) {
            competitionRepository.findByIdAndDeletedFalse(competitionId)
                    .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "关联竞赛不存在"));
        }
        try {
            String objectName = "documents/" + UUID.randomUUID() + "." + extension;
            String fileUrl = storageService.put(objectName, file.getContentType(), file.getBytes());
            DocumentEntity document = new DocumentEntity();
            document.setCompetitionId(competitionId);
            document.setTitle(StringUtils.hasText(title) ? title.trim() : filename);
            document.setFileType(extension.toUpperCase(Locale.ROOT));
            document.setFileUrl(fileUrl);
            document.setOriginalFilename(filename);
            document.setSourceUrl(StringUtils.hasText(sourceUrl) ? sourceUrl.trim() : null);
            document.setSourceDate(sourceDate);
            document.setDocumentType(StringUtils.hasText(documentType)
                    ? documentType.toUpperCase(Locale.ROOT) : "MATERIAL");
            document.setWorkYear(workYear);
            document.setStatus("PENDING");
            document = documentRepository.saveAndFlush(document);
            parseService.process(document.getId());
            return new DocumentUploadResult(document.getId(), document.getTitle(), "PROCESSING");
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.STORAGE_UNAVAILABLE, "文档上传失败: " + exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DocumentResponse> list(int page, int size, Long competitionId, String status) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1,
                Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DocumentEntity> result = StringUtils.hasText(status)
                ? documentRepository.findByStatus(status.toUpperCase(Locale.ROOT), pageable)
                : competitionId != null
                ? documentRepository.findByCompetitionId(competitionId, pageable)
                : documentRepository.findAll(pageable);
        return new PageResult<>(
                result.getContent().stream().map(DocumentResponse::from).toList(),
                result.getTotalElements(), Math.max(page, 1), pageable.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse get(Long id) {
        return DocumentResponse.from(requireDocument(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChunkResponse> chunks(Long id) {
        requireDocument(id);
        return chunkRepository.findByDocumentIdOrderByChunkIndex(id).stream()
                .map(chunk -> new ChunkResponse(
                        chunk.getId(), chunk.getChunkIndex(), chunk.getContent(),
                        chunk.getPageNumber(), chunk.getTokenCount(), chunk.getVectorId()))
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DocumentEntity document = requireDocument(id);
        vectorStoreService.deleteByDocumentId(id);
        chunkRepository.deleteByDocumentId(id);
        storageService.delete(document.getFileUrl());
        documentRepository.delete(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHit> search(SearchRequest request) {
        if (request.competitionId() != null) {
            Competition competition = competitionRepository.findByIdAndDeletedFalse(request.competitionId())
                    .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "竞赛不存在"));
            if ("EXPIRED".equalsIgnoreCase(competition.getStatus())
                    || competition.getContestEnd() != null
                    && competition.getContestEnd().isBefore(LocalDate.now())) {
                return List.of();
            }
        }
        List<Double> vector = embeddingClient.embed(List.of(request.query())).get(0);
        return vectorStoreService.search(vector, new SearchOptions(
                request.resolvedTopK(), request.competitionId(),
                request.sourceDateFrom(), request.documentType()));
    }

    private DocumentEntity requireDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "文档不存在"));
    }

    private String extension(String filename) {
        int position = filename.lastIndexOf('.');
        return position < 0 ? "" : filename.substring(position + 1).toLowerCase(Locale.ROOT);
    }
}

