package com.graduation.competition.rule.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.rule.dto.QaRequest;
import com.graduation.competition.rule.dto.QaResponse;
import com.graduation.competition.rule.service.RuleQaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleQaController {

    private final RuleQaService ruleQaService;

    @PostMapping("/qa")
    public ApiResponse<QaResponse> ask(@Valid @RequestBody QaRequest request) {
        return ApiResponse.success(ruleQaService.ask(request));
    }
}

