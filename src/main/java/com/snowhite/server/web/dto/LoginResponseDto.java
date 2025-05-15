package com.snowhite.server.web.dto;

import lombok.Builder;

@Builder
public record LoginResponseDto(String token) {
}