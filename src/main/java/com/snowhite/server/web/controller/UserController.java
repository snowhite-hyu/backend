package com.snowhite.server.web.controller;

import com.snowhite.server.web.dto.request.LoginRequestDto;
import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.web.dto.request.EmailDto;
import com.snowhite.server.web.dto.request.RegisterDto;
import com.snowhite.server.service.UserService;
import com.snowhite.server.web.dto.response.EmailCheckResponseDto;
import com.snowhite.server.web.dto.response.LoginResponseDto;
import com.snowhite.server.web.dto.response.RegisterResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Mono<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto,
                                                     ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        return userService.login(loginRequestDto)
            .map(token -> {
                response.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                return ApiResponse.onSuccess(
                        LoginResponseDto
                            .builder()
                            .token(token)
                            .build()
                );
            });
    }

    @GetMapping("/check-login")
    public Mono<ApiResponse<String>> checkLogin(Authentication authentication, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return userService.checkLogin(authentication, request)
                .map(ApiResponse::onSuccess);
    }

    @PostMapping("/register")
    public Mono<ApiResponse<RegisterResponseDto>> register(@RequestBody RegisterDto registerDto) {
        return userService.register(registerDto)
                .map( isSuccess -> {
                    String message = isSuccess ? "회원 가입에 성공하였습니다." : "회원 가입에 실패하였습니다. 이메일을 다시 확인해주세요.";
                    return ApiResponse.onSuccess(
                            RegisterResponseDto
                                    .builder()
                                    .isSuccess(isSuccess)
                                    .message(message)
                                    .build()
                    );
                });
    }

    @PostMapping("/check-email")
    public Mono<ApiResponse<EmailCheckResponseDto>> checkEmail(@RequestBody EmailDto emailDto) {
        return userService.checkEmail(emailDto)
            .map( isExisting -> {
                String message = isExisting ? "사용 가능한 이메일입니다." : "사용 중인 이메일입니다.";
                return ApiResponse.onSuccess(EmailCheckResponseDto
                        .builder()
                        .isExisting(isExisting)
                        .message(message)
                        .build());
            });
    }
}
