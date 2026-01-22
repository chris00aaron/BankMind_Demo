package com.naal.bankmind.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleDto {
    private Short id;
    private String codRole;
    private String name;
}
