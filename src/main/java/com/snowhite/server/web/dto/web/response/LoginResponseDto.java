package com.snowhite.server.web.dto.web.response;

import lombok.Builder;

@Builder
public record LoginResponseDto(String token) {
}