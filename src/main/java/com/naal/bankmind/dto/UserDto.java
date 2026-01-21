package com.naal.bankmind.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {

    private Long id;
    private String email;
    private String fullName;
    private String dni;
    private String phone;
    private String role;
    private String roleName;
}
