package com.naal.bankmind.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private boolean requiresMfa;
    private String mfaToken;
    private String message;
    private String phoneHint; // Últimos 4 dígitos del teléfono
}
