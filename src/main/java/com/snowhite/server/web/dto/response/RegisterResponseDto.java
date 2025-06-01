package com.snowhite.server.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResponseDto {
    public Boolean isSuccess;
    public String message;
}
