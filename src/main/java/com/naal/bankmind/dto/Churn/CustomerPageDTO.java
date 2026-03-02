package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta paginada para el endpoint GET /customers.
 * Incluye la página de clientes + KPIs globales pre-calculados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPageDTO {
    private List<CustomerDashboardDTO> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private DashboardKpisDTO kpis;
}
