package com.snowhite.server.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class RegisterDto {
    private String email;
    private String username;
    private String password;
}
