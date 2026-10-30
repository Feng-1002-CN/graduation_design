package com.graduation.competition.document.service;

import com.graduation.competition.common.PageResult;
import com.graduation.competition.document.dto.ChunkResponse;
import com.graduation.competition.document.dto.DocumentResponse;
import com.graduation.competition.document.dto.DocumentUploadResult;
import com.graduation.competition.document.dto.SearchRequest;
import com.graduation.competition.rag.vector.SearchHit;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DocumentService {

    DocumentUploadResult upload(
            MultipartFile file,
            Long competitionId,
            String title,
            String sourceUrl,
            LocalDate sourceDate,
            String documentType,
            String workYear);

    PageResult<DocumentResponse> list(int page, int size, Long competitionId, String status);

    DocumentResponse get(Long id);

    List<ChunkResponse> chunks(Long id);

    void delete(Long id);

    List<SearchHit> search(SearchRequest request);
}

