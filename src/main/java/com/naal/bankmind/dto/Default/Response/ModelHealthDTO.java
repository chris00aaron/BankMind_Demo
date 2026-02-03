package com.naal.bankmind.dto.Default.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la vista de monitoreo del modelo en producción.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelHealthDTO {

    // Estado del modelo
    private String version;
    private LocalDateTime deploymentDate;
    private long daysActive;
    private boolean isActive;

    // Métricas principales (simplificadas)
    private MetricasModelo metricas;

    // Arquitectura del ensamble
    private ArquitecturaModelo arquitectura;

    // Tendencia de rendimiento
    private List<TendenciaRendimiento> tendencia;

    // Info del dataset
    private DatasetResumen dataset;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricasModelo {
        private double aucRoc;
        private double precision;
        private double recall;
        private double f1Score;
        private double giniCoefficient;
        private double ksStatistic;
        private double accuracy;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ArquitecturaModelo {
        private String tipo; // "VotingClassifier"
        private String estrategia; // "soft"
        private List<ComponenteModelo> componentes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ComponenteModelo {
        private String nombre;
        private int peso;
        private Map<String, Object> parametros;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TendenciaRendimiento {
        private String mes;
        private double morosidadReal;
        private double prediccion;
        private double diferencia; // prediccion - real
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatasetResumen {
        private int totalRegistros;
        private int datosEntrenamiento;
        private int datosPrueba;
        private LocalDateTime fechaDataset;
        private String fuente;
    }
}
