package com.naal.bankmind.dto.Login;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditLoginDTO {
    private Long id;
    private Long userId;
    private String email;
    private String roleName;
    private String ipAddress;
    private String userAgent;
    private String loginStatus;
    private String failureReason;
    private LocalDateTime loginAt;
}
