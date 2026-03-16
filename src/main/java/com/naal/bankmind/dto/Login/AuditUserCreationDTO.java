package com.naal.bankmind.dto.Login;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditUserCreationDTO {
    private Long id;
    private Long createdUserId;
    private String createdUserEmail;
    private String createdUserRole;
    private Long adminUserId;
    private String adminEmail;
    private String ipAddress;
    private LocalDateTime createdAt;
}
