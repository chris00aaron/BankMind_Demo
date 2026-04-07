package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta del endpoint GET /api/v1/churn/risk-intelligence.
 * Contiene la muestra activa y los metadatos del lote.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskIntelligenceDTO {

    /** Clientes de la muestra activa. Vacío si no hay lote activo. */
    private List<RiskClientDTO> clients;

    /** Fecha/hora en que se generó el lote activo. */
    private LocalDateTime lastUpdated;

    /** 'scheduler' | 'manual' */
    private String triggeredBy;

    /** Número de clientes en la muestra (puede ser < targetSize si la BD tiene pocos). */
    private int sampleSize;

    /** Total de clientes en BD al momento del muestreo. */
    private long totalCustomers;

    /** false si no hay lote activo todavía. */
    private boolean hasSample;
}
