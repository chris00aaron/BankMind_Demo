package com.naal.bankmind.dto.Login;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendOtpRequest {

    @NotBlank(message = "El token MFA es requerido")
    private String mfaToken;
}
