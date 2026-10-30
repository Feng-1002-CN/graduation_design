package com.graduation.competition.workbench.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.common.PageResult;
import com.graduation.competition.workbench.dto.WorkbenchVO;
import com.graduation.competition.workbench.service.WorkbenchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workbench")
@RequiredArgsConstructor
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    @GetMapping("/projects/{projectId}")
    public ApiResponse<WorkbenchVO> get(@PathVariable Long projectId) {
        return ApiResponse.success(workbenchService.get(projectId));
    }

    @GetMapping("/projects")
    public ApiResponse<PageResult<WorkbenchVO.ProjectSummary>> projects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(workbenchService.list(page, size));
    }
}

