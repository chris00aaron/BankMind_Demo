package com.naal.bankmind.dto.Default.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO principal para la vista de Estrategias de Mitigación.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyResponseDTO {

    private StrategySummary resumen;
    private List<SegmentSummary> segmentos;

    /**
     * Resumen general de la cartera.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategySummary {
        private long totalCuentas;
        private double perdidaTotal;
        private double tasaMorosidad;
    }

    /**
     * Resumen de un segmento de riesgo.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentSummary {
        private String segmento; // Crítico, Alto, Medio, Bajo
        private long totalCuentas;
        private double perdidaEstimada;
        private double probabilidadPromedio; // Prob de pago promedio del segmento
        private String factorPrincipal; // Factor de riesgo más frecuente
    }

    /**
     * Resultado de simular una campaña sobre un segmento.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationResult {
        private String segmento;
        private String campaignName;
        private long totalCuentasSegmento;
        private double perdidaActual;
        private double perdidaProyectada;
        private double reduccionPerdida; // Porcentaje de reducción
        private double tasaMorosidadActual;
        private double tasaMorosidadProyectada;
        private long cuentasMejoradas; // Cuentas que cambian de categoría SBS
        private double costoTotal;
        private double roi; // (ahorro - costo) / costo
    }
}
