package com.graduation.competition.document.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.common.PageResult;
import com.graduation.competition.document.dto.ChunkResponse;
import com.graduation.competition.document.dto.DocumentResponse;
import com.graduation.competition.document.dto.DocumentUploadResult;
import com.graduation.competition.document.dto.SearchRequest;
import com.graduation.competition.document.service.DocumentService;
import com.graduation.competition.rag.vector.SearchHit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResult> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sourceDate,
            @RequestParam(defaultValue = "MATERIAL") String documentType,
            @RequestParam(required = false) String workYear) {
        return ApiResponse.success(documentService.upload(
                file, competitionId, title, sourceUrl, sourceDate, documentType, workYear));
    }

    @GetMapping
    public ApiResponse<PageResult<DocumentResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(documentService.list(page, size, competitionId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> get(@PathVariable Long id) {
        return ApiResponse.success(documentService.get(id));
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<List<ChunkResponse>> chunks(@PathVariable Long id) {
        return ApiResponse.success(documentService.chunks(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/search")
    public ApiResponse<Map<String, List<SearchHit>>> search(
            @Valid @RequestBody SearchRequest request) {
        return ApiResponse.success(Map.of("hits", documentService.search(request)));
    }
}

