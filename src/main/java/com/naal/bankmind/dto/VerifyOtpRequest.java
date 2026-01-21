package com.naal.bankmind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "El token MFA es requerido")
    private String mfaToken;

    @NotBlank(message = "El código OTP es requerido")
    @Size(min = 6, max = 6, message = "El código OTP debe tener 6 dígitos")
    private String code;
}
