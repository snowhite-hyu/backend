package com.snowhite.server.web.controller;

import com.snowhite.server.web.dto.web.request.LoginRequestDto;
import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.web.dto.EmailDto;
import com.snowhite.server.web.dto.web.request.RegisterDto;
import com.snowhite.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Mono<ApiResponse<String>> login(@RequestBody LoginRequestDto loginRequestDto) {
        return userService.login(loginRequestDto);
    }

    @GetMapping("/check-login")
    public Mono<ApiResponse<String>> checkLogin(Authentication authentication, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return userService.checkLogin(authentication, request);
    }

    @PostMapping("/register")
    public Mono<ApiResponse<String>> register(@RequestBody RegisterDto registerDto) {
        return userService.register(registerDto);
    }

    @PostMapping("/check-email")
    public Mono<ApiResponse<String>> checkEmail(@RequestBody EmailDto emailDto) {
        return userService.checkEmail(emailDto);
    }
}
