package com.naal.bankmind.scheduler;

import com.naal.bankmind.service.Fraud.FraudClusteringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler de Clustering de Fraude.
 *
 * Ejecuta el análisis K-Means semanalmente (Lunes 03:00 AM),
 * después del training scheduler (Lunes 02:00 AM) para asegurar
 * que los perfiles reflejan el modelo más reciente.
 *
 * Sigue el mismo patrón que {@link FraudTrainingScheduler}:
 * - Constructor injection (testeable).
 * - @Scheduled con cron expression para máxima flexibilidad.
 * - Logging detallado de inicio/fin/error.
 * - Sin lógica de negocio propia — delega completamente al servicio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClusteringScheduler {

    private final FraudClusteringService clusteringService;

    /**
     * Ejecución automática: Lunes a las 03:00 AM (30 min después del training).
     *
     * Cron: "segundo minuto hora díaMes mes díaSemana"
     * MON = 2 en estándar Spring (1=DOM, 2=LUN).
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void runWeeklyClustering() {
        log.info("========================================");
        log.info("[CLUSTERING SCHEDULER] Iniciando análisis semanal de perfiles de fraude.");
        log.info("========================================");

        long start = System.currentTimeMillis();
        try {
            var profiles = clusteringService.computeAndPersist();

            long elapsed = System.currentTimeMillis() - start;
            log.info("[CLUSTERING SCHEDULER] Completado en {}ms — {} perfiles generados.",
                    elapsed, profiles.size());

            if (profiles.isEmpty()) {
                log.warn("[CLUSTERING SCHEDULER] No se generaron perfiles. " +
                        "Posiblemente no hay suficientes transacciones ALTO RIESGO.");
            } else {
                profiles.forEach(p -> log.info("  -> Cluster {}: {} ({} fraudes, {:.1f}%)",
                        p.getClusterId(), p.getLabel(), p.getFraudCount(), p.getPctOfTotal()));
            }

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[CLUSTERING SCHEDULER] Error tras {}ms: {}", elapsed, ex.getMessage(), ex);
            // No relanzamos la excepción para que el scheduler no se deshabilite
        }
    }
}
