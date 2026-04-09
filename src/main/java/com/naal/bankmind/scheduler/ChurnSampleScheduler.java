package com.naal.bankmind.scheduler;

import com.naal.bankmind.service.Churn.ChurnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler nocturno para el muestreo estratificado de "Inteligencia de Riesgo".
 *
 * Ejecución: diariamente a las 02:00 AM (configurable en application.properties).
 * Selecciona ~500 clientes estratificados por país × cuartil de balance,
 * los analiza con el modelo Python y persiste el lote activo.
 *
 * El analista llega por la mañana con datos frescos sin necesidad de acción manual.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChurnSampleScheduler {

    private final ChurnService churnService;

    @Value("${churn.sample.size:500}")
    private int sampleSize;

    /**
     * Tarea diaria a las 02:00 AM.
     * Cron configurable en application.properties: churn.sample.cron
     */
    @Scheduled(cron = "${churn.sample.cron:0 0 2 * * *}")
    public void buildNightlySample() {
        log.info("[ChurnSampleScheduler] Iniciando muestreo nocturno. targetSize={}", sampleSize);
        try {
            churnService.buildRiskSample(sampleSize, "scheduler");
            log.info("[ChurnSampleScheduler] Muestreo nocturno completado.");
        } catch (Exception e) {
            log.error("[ChurnSampleScheduler] Error en muestreo nocturno: {}", e.getMessage(), e);
        }
    }
}
