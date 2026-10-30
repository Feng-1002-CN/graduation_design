package com.graduation.competition.apply.controller;

import com.graduation.competition.apply.dto.ApplyCheckRequest;
import com.graduation.competition.apply.dto.ApplyCheckResponse;
import com.graduation.competition.apply.dto.ApplyFile;
import com.graduation.competition.apply.service.ApplyCheckService;
import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.storage.ObjectStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apply")
@RequiredArgsConstructor
public class ApplyCheckController {

    private final ApplyCheckService checkService;
    private final ObjectStorageService storageService;

    @PostMapping("/check")
    public ApiResponse<ApplyCheckResponse> check(@Valid @RequestBody ApplyCheckRequest request) {
        return ApiResponse.success(checkService.check(request));
    }

    @GetMapping("/check/{id}")
    public ApiResponse<ApplyCheckResponse> get(@PathVariable Long id) {
        return ApiResponse.success(checkService.get(id));
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ApplyFile> upload(@RequestPart("file") MultipartFile file) throws Exception {
        String type = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!Set.of("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/zip", "image/png", "image/jpeg").contains(type)) {
            throw new BizException(ErrorCode.INVALID_FILE, "不支持的申报材料格式");
        }
        String name = file.getOriginalFilename() == null ? "material" : file.getOriginalFilename();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        String url = storageService.put("apply/" + UUID.randomUUID() + extension, type, file.getBytes());
        return ApiResponse.success(new ApplyFile(name, type, url));
    }
}

