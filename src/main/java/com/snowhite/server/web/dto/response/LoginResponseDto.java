package com.snowhite.server.web.dto.response;

import lombok.Builder;

@Builder
public record LoginResponseDto(String token) {
}