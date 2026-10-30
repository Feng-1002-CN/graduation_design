package com.graduation.competition.plan.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.plan.dto.PlanDTO;
import com.graduation.competition.plan.dto.PlanGenerateRequest;
import com.graduation.competition.plan.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping("/generate")
    public ApiResponse<PlanDTO> generate(@Valid @RequestBody PlanGenerateRequest request) {
        return ApiResponse.success(planService.generate(request.projectId()));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<PlanDTO> get(@PathVariable Long projectId) {
        return ApiResponse.success(planService.getByProjectId(projectId));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanDTO> update(@PathVariable Long id, @RequestBody PlanDTO dto) {
        return ApiResponse.success(planService.update(id, dto));
    }
}

