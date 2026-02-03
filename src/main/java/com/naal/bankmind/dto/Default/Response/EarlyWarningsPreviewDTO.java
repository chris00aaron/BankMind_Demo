package com.naal.bankmind.dto.Default.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO para preview de alertas tempranas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarlyWarningsPreviewDTO {

    private long totalClientesEnAlerta;
    private double totalDineroEnRiesgo;
    private List<AlertaDTO> alertas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertaDTO {
        private String id;
        private String tipo; // critico, alto, tendencia, vencimiento
        private String titulo;
        private String descripcion;
        private int clientesAfectados;
        private double dineroEnRiesgo;
        private String prioridad; // urgente, alta, media
        private String fecha;
        private String accionRecomendada;
    }
}
