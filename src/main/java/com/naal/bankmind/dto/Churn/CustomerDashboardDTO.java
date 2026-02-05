package com.naal.bankmind.dto.Churn;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer dashboard display in the Churn module.
 * Contains all fields needed by the frontend table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDashboardDTO {
    private Long id;
    private Integer score;
    private Integer age;
    private BigDecimal balance;
    private String country;
    private String name;
    private Integer risk;
}
