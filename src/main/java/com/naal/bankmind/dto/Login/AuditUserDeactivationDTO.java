package com.naal.bankmind.dto.Login;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditUserDeactivationDTO {
    private Long id;
    private Long deactivatedUserId;
    private String deactivatedUserEmail;
    private String deactivatedUserRole;
    private Long adminUserId;
    private String adminEmail;
    private String ipAddress;
    private LocalDateTime deactivatedAt;
}
