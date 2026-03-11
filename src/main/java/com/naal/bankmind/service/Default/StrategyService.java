package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Response.StrategyResponseDTO.*;
import com.naal.bankmind.entity.Default.DefaultPolicies;
import com.naal.bankmind.entity.Default.MitigationCampaign;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;
import com.naal.bankmind.repository.Default.MitigationCampaignRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.*;

/**
 * Servicio para la vista de Estrategias de Mitigación de Riesgo.
 * Provee resumen por segmentos y simulación de impacto de campañas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final DefaultPoliciesRepository policiesRepository;
    private final MitigationCampaignRepository campaignRepository;
    private final EntityManager entityManager;

    // ============ RESUMEN POR SEGMENTOS ============

    /**
     * Obtiene el resumen agregado de la cartera por segmento de riesgo.
     * Usa SQL agregado directamente → NO carga todas las predicciones.
     */
    @SuppressWarnings("unchecked")
    public List<SegmentSummary> getSegmentsSummary() {
        log.info("Calculando resumen por segmentos de riesgo");

        String nativeQuery = """
                WITH latest AS (
                    SELECT dp.id_prediction, dp.default_probability, dp.estimated_loss, dp.main_risk_factor,
                           mh.record_id, mh.pay_x,
                           ROW_NUMBER() OVER (PARTITION BY mh.record_id ORDER BY dp.date_prediction DESC) as rn
                    FROM default_prediction dp
                    JOIN monthly_history mh ON mh.id_historial = dp.id_historial
                    WHERE dp.default_probability IS NOT NULL
                ),
                classified AS (
                    SELECT
                        (1.0 - default_probability) * 100 as prob_pago,
                        estimated_loss,
                        main_risk_factor,
                        CASE
                            WHEN pay_x <= 0 THEN 'Normal'
                            WHEN pay_x = 1  THEN 'CPP'
                            WHEN pay_x = 2  THEN 'Deficiente'
                            WHEN pay_x <= 4  THEN 'Dudoso'
                            ELSE 'Pérdida'
                        END as segmento
                    FROM latest WHERE rn = 1
                )
                SELECT
                    segmento,
                    COUNT(*) as total_cuentas,
                    COALESCE(SUM(estimated_loss), 0) as perdida_estimada,
                    AVG(prob_pago) as prob_promedio,
                    MODE() WITHIN GROUP (ORDER BY main_risk_factor) as factor_principal
                FROM classified
                GROUP BY segmento
                ORDER BY
                    CASE segmento
                        WHEN 'Pérdida' THEN 1
                        WHEN 'Dudoso' THEN 2
                        WHEN 'Deficiente' THEN 3
                        WHEN 'CPP' THEN 4
                        WHEN 'Normal' THEN 5
                    END
                """;

        List<Object[]> results = entityManager.createNativeQuery(nativeQuery).getResultList();

        // Asegurarnos de que todos los segmentos estén presentes
        Map<String, SegmentSummary> segmentMap = new LinkedHashMap<>();
        segmentMap.put("Pérdida", new SegmentSummary("Pérdida", 0, 0.0, 0.0, "—"));
        segmentMap.put("Dudoso", new SegmentSummary("Dudoso", 0, 0.0, 0.0, "—"));
        segmentMap.put("Deficiente", new SegmentSummary("Deficiente", 0, 0.0, 0.0, "—"));
        segmentMap.put("CPP", new SegmentSummary("CPP", 0, 0.0, 0.0, "—"));
        segmentMap.put("Normal", new SegmentSummary("Normal", 0, 0.0, 0.0, "—"));

        for (Object[] row : results) {
            String segmento = (String) row[0];
            long total = ((Number) row[1]).longValue();
            double perdida = ((Number) row[2]).doubleValue();
            double probPromedio = ((Number) row[3]).doubleValue();
            String factor = row[4] != null ? (String) row[4] : "—";

            if ("Batch".equalsIgnoreCase(factor) || "Unknown".equalsIgnoreCase(factor)) {
                factor = "—";
            }

            segmentMap.put(segmento, new SegmentSummary(
                    segmento, total, Math.round(perdida * 100.0) / 100.0,
                    Math.round(probPromedio * 10.0) / 10.0, factor));
        }

        log.info("Segmentos calculados: {}", segmentMap.keySet());
        return new ArrayList<>(segmentMap.values());
    }

    /**
     * Obtiene el resumen general (total cuentas, pérdida total, tasa morosidad).
     */
    @SuppressWarnings("unchecked")
    public StrategySummary getGeneralSummary() {
        double threshold = getDefaultThreshold();

        String nativeQuery = """
                WITH latest AS (
                    SELECT dp.default_probability, dp.estimated_loss,
                           ROW_NUMBER() OVER (PARTITION BY mh.record_id ORDER BY dp.date_prediction DESC) as rn
                    FROM default_prediction dp
                    JOIN monthly_history mh ON mh.id_historial = dp.id_historial
                    WHERE dp.default_probability IS NOT NULL
                )
                SELECT
                    COUNT(*) as total,
                    COALESCE(SUM(estimated_loss), 0) as perdida_total,
                    COUNT(CASE WHEN default_probability > :threshold THEN 1 END) as morosos
                FROM latest WHERE rn = 1
                """;

        Object[] row = (Object[]) entityManager.createNativeQuery(nativeQuery)
                .setParameter("threshold", threshold)
                .getSingleResult();

        long total = ((Number) row[0]).longValue();
        double perdida = ((Number) row[1]).doubleValue();
        long morosos = ((Number) row[2]).longValue();
        double tasa = total > 0 ? Math.round((morosos * 1000.0 / total)) / 10.0 : 0.0;

        return new StrategySummary(total, Math.round(perdida * 100.0) / 100.0, tasa);
    }

    // ============ SIMULACIÓN DE CAMPAÑA ============

    /**
     * Simula el impacto de aplicar una campaña a un segmento específico.
     */
    @SuppressWarnings("unchecked")
    public SimulationResult simulateCampaign(Long campaignId, String segmento) {
        MitigationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaña no encontrada: " + campaignId));

        log.info("Simulando campaña '{}' sobre segmento '{}'", campaign.getCampaignName(), segmento);

        double reductionFactor = campaign.getReductionFactor().doubleValue();
        double costPerAccount = campaign.getEstimatedCost() != null ? campaign.getEstimatedCost().doubleValue() : 0;
        double threshold = getDefaultThreshold();
        double lgdFactor = getLgdFactor();

        // Obtener datos del segmento a nivel de cada cuenta
        String nativeQuery = """
                WITH latest AS (
                    SELECT dp.default_probability, dp.estimated_loss,
                           mh.bill_amt_x, mh.record_id, mh.pay_x,
                           ROW_NUMBER() OVER (PARTITION BY mh.record_id ORDER BY dp.date_prediction DESC) as rn
                    FROM default_prediction dp
                    JOIN monthly_history mh ON mh.id_historial = dp.id_historial
                    WHERE dp.default_probability IS NOT NULL
                ),
                classified AS (
                    SELECT
                        default_probability,
                        estimated_loss,
                        bill_amt_x,
                        CASE
                            WHEN pay_x <= 0 THEN 'Normal'
                            WHEN pay_x = 1  THEN 'CPP'
                            WHEN pay_x = 2  THEN 'Deficiente'
                            WHEN pay_x <= 4  THEN 'Dudoso'
                            ELSE 'Pérdida'
                        END as segmento
                    FROM latest WHERE rn = 1
                )
                SELECT default_probability, estimated_loss, bill_amt_x
                FROM classified
                WHERE segmento = :segmento
                """;

        List<Object[]> cuentas = entityManager.createNativeQuery(nativeQuery)
                .setParameter("segmento", segmento)
                .getResultList();

        if (cuentas.isEmpty()) {
            return new SimulationResult(segmento, campaign.getCampaignName(),
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        // Obtener totales globales para calcular tasa de morosidad
        StrategySummary general = getGeneralSummary();
        long totalGlobal = general.getTotalCuentas();

        double perdidaActual = 0;
        double perdidaProyectada = 0;
        long morososActuales = 0;
        long morososProyectados = 0;
        long cuentasMejoradas = 0;

        for (Object[] cuenta : cuentas) {
            double probDefault = ((Number) cuenta[0]).doubleValue();
            double lossActual = cuenta[1] != null ? ((Number) cuenta[1]).doubleValue() : 0;
            double ead = cuenta[2] != null ? Math.abs(((Number) cuenta[2]).doubleValue()) : 0;

            perdidaActual += lossActual;

            // Simular reducción
            double newProbDefault = probDefault * (1.0 - reductionFactor);
            double newLoss = ead * newProbDefault * lgdFactor;
            perdidaProyectada += newLoss;

            // Contar morosos actuales y proyectados
            if (probDefault > threshold)
                morososActuales++;
            if (newProbDefault > threshold)
                morososProyectados++;

            // Cuentas que mejoran categoría SBS
            String sbsActual = getSBSCategory(probDefault);
            String sbsNuevo = getSBSCategory(newProbDefault);
            if (!sbsActual.equals(sbsNuevo))
                cuentasMejoradas++;
        }

        long totalCuentas = cuentas.size();
        double costoTotal = totalCuentas * costPerAccount;
        double ahorro = perdidaActual - perdidaProyectada;
        double roi = costoTotal > 0
                ? Math.round((ahorro / costoTotal) * 10.0) / 10.0
                : (ahorro > 0 ? -1 : 0); // -1 = marcador "Sin costo"

        // Calcular tasa de morosidad global ajustada
        // Morosos globales actuales - morosos que dejan de serlo en este segmento
        long morososGlobalesActual = Math.round(general.getTasaMorosidad() * totalGlobal / 100.0);
        long morososMenosEnSegmento = morososActuales - morososProyectados;
        long morososGlobalesProyectados = morososGlobalesActual - morososMenosEnSegmento;

        double tasaActual = general.getTasaMorosidad();
        double tasaProyectada = totalGlobal > 0
                ? Math.round((morososGlobalesProyectados * 1000.0 / totalGlobal)) / 10.0
                : 0;

        double reduccionPct = perdidaActual > 0
                ? Math.round((1.0 - perdidaProyectada / perdidaActual) * 1000.0) / 10.0
                : 0;

        double probPromedioActual = cuentas.size() > 0
                ? (cuentas.stream().mapToDouble(c -> (1.0 - ((Number) c[0]).doubleValue())).sum() / cuentas.size())
                        * 100
                : 0;

        double probPromedioProyectada = cuentas.size() > 0
                ? (cuentas.stream().mapToDouble(c -> (1.0 - ((Number) c[0]).doubleValue() * (1.0 - reductionFactor)))
                        .sum() / cuentas.size()) * 100
                : 0;

        log.info("Simulación: pérdida {} → {}, morosidad {}% → {}%, ROI: {}x",
                perdidaActual, perdidaProyectada, tasaActual, tasaProyectada, roi);

        return new SimulationResult(
                segmento,
                campaign.getCampaignName(),
                totalCuentas,
                Math.round(perdidaActual * 100.0) / 100.0,
                Math.round(perdidaProyectada * 100.0) / 100.0,
                reduccionPct,
                Math.round(probPromedioActual * 10.0) / 10.0,
                Math.round(probPromedioProyectada * 10.0) / 10.0,
                tasaActual,
                tasaProyectada,
                cuentasMejoradas,
                Math.round(costoTotal * 100.0) / 100.0,
                roi);
    }

    // ============ HELPERS ============

    private double getDefaultThreshold() {
        return policiesRepository.findByIsActiveTrue()
                .map(p -> p.getThresholdApproval().doubleValue())
                .orElse(0.5);
    }

    private double getLgdFactor() {
        return policiesRepository.findByIsActiveTrue()
                .map(p -> p.getFactorLgd().doubleValue())
                .orElse(0.45);
    }

    private String getSBSCategory(double probDefault) {
        DefaultPolicies policy = policiesRepository.findByIsActiveTrue().orElse(null);
        if (policy != null && policy.getSbsClassificationMatrix() != null) {
            for (var rule : policy.getSbsClassificationMatrix()) {
                if (probDefault >= rule.getMin() && probDefault < rule.getMax()) {
                    return rule.getCategoria();
                }
            }
        }
        // Fallback
        if (probDefault <= 0.05)
            return "Normal";
        if (probDefault <= 0.25)
            return "CPP";
        if (probDefault <= 0.60)
            return "Deficiente";
        if (probDefault <= 0.90)
            return "Dudoso";
        return "Pérdida";
    }
}
