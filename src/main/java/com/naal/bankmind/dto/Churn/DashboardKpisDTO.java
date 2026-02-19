package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * KPIs globales del dashboard, calculados server-side
 * para evitar traer todos los clientes al frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpisDTO {
    private long totalCustomers;
    private long customersAtRisk; // clientes con risk >= 50
    private BigDecimal capitalAtRisk; // SUM(balance * risk/100) donde risk >= 50
    private double retentionRate; // % clientes con risk < 50
    private List<ScatterPointDTO> scatterData; // Top N para el chart

    // Risk distribution for donut chart
    private long highRiskCount; // risk > 70
    private long mediumRiskCount; // risk 50-70
    private long lowRiskCount; // risk < 50

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScatterPointDTO {
        private double x; // Probabilidad de Fuga (risk)
        private double y; // Balance
        private double z; // Tamaño burbuja (fijo 100)
        private String name;
        private long id;
        private String country; // País para tooltip enriquecido
    }
}
