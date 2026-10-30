package com.graduation.competition.task.controller;

import com.graduation.competition.common.ApiResponse;
import com.graduation.competition.task.dto.TaskResponse;
import com.graduation.competition.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> get(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.get(taskId));
    }
}

