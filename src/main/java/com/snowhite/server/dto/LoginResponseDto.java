package com.snowhite.server.dto;

import lombok.Builder;

@Builder
public record LoginResponseDto(String token) {
}