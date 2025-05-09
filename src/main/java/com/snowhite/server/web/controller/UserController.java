package com.snowhite.server.web.controller;

import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.dto.EmailDto;
import com.snowhite.server.dto.RegisterDto;
import com.snowhite.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Mono<ApiResponse<String>> register(@RequestBody RegisterDto registerDto) {
        return userService.register(registerDto);
    }

    @PostMapping("/check-email")
    public Mono<ApiResponse<String>> checkEmail(@RequestBody EmailDto emailDto) {
        return userService.checkEmail(emailDto);
    }
}
