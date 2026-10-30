package com.graduation.competition.compare.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.compare.dto.PastWorkCompareRequest;
import com.graduation.competition.compare.dto.PastWorkCompareResult;
import com.graduation.competition.compare.service.PastWorkCompareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/compare")
@RequiredArgsConstructor
public class PastWorkCompareController {

    private final PastWorkCompareService compareService;

    @PostMapping("/past-works")
    public ApiResponse<Map<String, List<PastWorkCompareResult>>> compare(
            @Valid @RequestBody PastWorkCompareRequest request) {
        return ApiResponse.success(Map.of("works", compareService.compare(request)));
    }
}

