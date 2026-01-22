package com.naal.bankmind.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserListDto {
    private Long id;
    private String dni;
    private String fullName;
    private String email;
    private String phone;
    private String roleCodRole;
    private String roleName;
    private Boolean enable;
    private LocalDateTime lastAccess;
    private LocalDateTime createdAt;
}
