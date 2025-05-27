package com.snowhite.server.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailCheckResponseDto {
    private Boolean isExisting;
    private String message;
}