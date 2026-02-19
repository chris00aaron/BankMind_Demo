package com.naal.bankmind.dto.Default.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * DTO para la respuesta del endpoint de gestión de cartera.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponseDTO {

    private PortfolioResumen resumen;
    private List<PortfolioCuenta> cuentas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioResumen {
        private long totalCuentas;
        private double exposicionTotal;
        private Map<String, Long> distribucionSBS;
        private Map<String, Long> distribucionRiesgo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioCuenta {
        private Long recordId;
        private String nombre;
        private Integer edad;
        private String educacion;
        private String estadoCivil;
        private double probabilidadPago;
        private String nivelRiesgo;
        private String clasificacionSBS;
        private double estimatedLoss;
        private String mainRiskFactor;
        private String fechaPrediccion;
    }
}
