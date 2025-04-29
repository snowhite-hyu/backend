package com.snowhite.server.web.controller;

import com.snowhite.server.payload.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health-check")
public class HealthCheckController {

    @GetMapping
    public ApiResponse<?> healthCheck() {
        return ApiResponse.onSuccess("Server Running");
    }
}
