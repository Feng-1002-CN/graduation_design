package com.graduation.competition.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.common.PageResult;
import com.graduation.competition.dto.CompetitionQuery;
import com.graduation.competition.dto.CompetitionRequest;
import com.graduation.competition.dto.CompetitionResponse;
import com.graduation.competition.service.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "竞赛信息")
@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    @Operation(summary = "新增竞赛")
    @PostMapping
    public ApiResponse<CompetitionResponse> create(
            @Valid @RequestBody CompetitionRequest request) {
        return ApiResponse.success(competitionService.create(request));
    }

    @Operation(summary = "分页查询竞赛")
    @GetMapping
    public ApiResponse<PageResult<CompetitionResponse>> query(
            @Valid CompetitionQuery query) {
        return ApiResponse.success(competitionService.query(query));
    }

    @Operation(summary = "获取竞赛详情")
    @GetMapping("/{id}")
    public ApiResponse<CompetitionResponse> get(@PathVariable Long id) {
        return ApiResponse.success(competitionService.get(id));
    }

    @Operation(summary = "更新竞赛")
    @PutMapping("/{id}")
    public ApiResponse<CompetitionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionRequest request) {
        return ApiResponse.success(competitionService.update(id, request));
    }

    @Operation(summary = "逻辑删除竞赛")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        competitionService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询竞赛有效规则")
    @GetMapping("/{id}/rules")
    public ApiResponse<List<CompetitionResponse.RuleResponse>> rules(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponse.success(competitionService.getRules(id, activeOnly));
    }

    @Operation(summary = "新增竞赛规则")
    @PostMapping("/{id}/rules")
    public ApiResponse<CompetitionResponse.RuleResponse> addRule(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionRequest.RuleRequest request) {
        return ApiResponse.success(competitionService.addRule(id, request));
    }

    @Operation(summary = "更新竞赛规则")
    @PutMapping("/rules/{ruleId}")
    public ApiResponse<CompetitionResponse.RuleResponse> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody CompetitionRequest.RuleRequest request) {
        return ApiResponse.success(competitionService.updateRule(ruleId, request));
    }

    @Operation(summary = "获取组别详情")
    @GetMapping("/groups/{groupId}")
    public ApiResponse<CompetitionResponse.GroupResponse> group(@PathVariable Long groupId) {
        return ApiResponse.success(competitionService.getGroup(groupId));
    }
}

