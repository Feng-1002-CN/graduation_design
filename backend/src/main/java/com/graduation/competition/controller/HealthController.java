package com.graduation.competition.controller;

import com.graduation.competition.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 基础服务健康检查，不触发外部中间件连接。
 */
@Tag(name = "系统")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "服务健康检查")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "timestamp", Instant.now()));
    }
}

