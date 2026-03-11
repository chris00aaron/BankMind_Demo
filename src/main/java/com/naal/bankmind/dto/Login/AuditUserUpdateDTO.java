package com.naal.bankmind.dto.Login;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditUserUpdateDTO {
    private Long id;
    private Long updatedUserId;
    private String updatedUserEmail;
    private Long adminUserId;
    private String adminEmail;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime updatedAt;
}
