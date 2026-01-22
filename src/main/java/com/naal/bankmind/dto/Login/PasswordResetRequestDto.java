package com.naal.bankmind.dto.Login;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PasswordResetRequestDto {

    private Long id;
    private String userEmail;
    private String userName;
    private String userDni;
    private LocalDateTime requestedAt;
    private String status;
}
