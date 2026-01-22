package com.naal.bankmind.dto.Login;

import com.naal.bankmind.dto.Shared.UserDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserDto user;
    private String message;
}
