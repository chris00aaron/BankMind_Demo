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

    // New fields for real data (replacing frontend mocks)
    private Integer tenure; // Years as customer (calculated from id_registration_date)
    private String since; // Year customer registered
    private Integer products; // Number of credit cards/products
    private String email; // Contact email from customer table
}
