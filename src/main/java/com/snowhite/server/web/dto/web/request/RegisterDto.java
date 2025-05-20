package com.snowhite.server.web.dto.web.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class RegisterDto {
    private String email;
    private String username;
    private String password;
}
