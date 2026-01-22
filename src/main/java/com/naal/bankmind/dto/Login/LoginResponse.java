package com.naal.bankmind.dto.Login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private boolean requiresMfa;
    private boolean requiresPasswordChange;
    private String mfaToken;
    private String accessToken; // Token temporal para cambio de contraseña
    private Long userId;
    private String message;
    private String phoneHint; // Últimos 4 dígitos del teléfono
}
