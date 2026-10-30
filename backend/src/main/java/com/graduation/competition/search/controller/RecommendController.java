package com.graduation.competition.search.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.search.dto.RecommendRequest;
import com.graduation.competition.search.dto.RecommendResult;
import com.graduation.competition.search.service.RecommendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @PostMapping("/recommend")
    public ApiResponse<Map<String, List<RecommendResult>>> recommend(
            @Valid @RequestBody RecommendRequest request) {
        return ApiResponse.success(Map.of("recommendations", recommendService.recommend(request)));
    }
}

