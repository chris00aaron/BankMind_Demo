package com.naal.bankmind.dto.Default.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO principal que contiene todos los datos del dashboard de morosidad.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMorosidadDTO {

    private MetricasResumen metricas;
    private MetricasModelo modelo;
    private List<DistribucionProbabilidad> distribucionProbabilidad;
    private List<SegmentacionRiesgo> segmentacionRiesgo;
    private List<TendenciaMensual> tendenciaMensual;
    private List<ClienteAltoRiesgo> clientesAltoRiesgo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasResumen {
        private long totalClientes;
        private long clientesEnRiesgo;
        private double dineroEnRiesgo;
        private double tasaMorosidadPredicha;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasModelo {
        private double precision;
        private double recall;
        private double f1Score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistribucionProbabilidad {
        private String rango;
        private long cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentacionRiesgo {
        private String nivel;
        private long cantidad;
        private double dinero;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendenciaMensual {
        private String mes;
        private double morosidad;
        private double prediccion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteAltoRiesgo {
        private Long id;
        private String nombre;
        private double probabilidadPago;
        private String nivelRiesgo;
        private double montoCuota;
        private int cuotasAtrasadas;
    }
}
