package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Response.EarlyWarningsPreviewDTO;
import com.naal.bankmind.dto.Default.Response.EarlyWarningsPreviewDTO.AlertaDTO;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.Default.DefaultPolicies;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;
import com.naal.bankmind.repository.Default.DefaultPredictionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para generar preview de alertas tempranas usando las últimas
 * predicciones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EarlyWarningsService {

        private final DefaultPredictionRepository predictionRepository;
        private final DefaultPoliciesRepository policiesRepository;
        private final EntityManager entityManager;

        /**
         * Obtiene el umbral de default de la política activa.
         * Si no hay política activa, usa 0.5 como default.
         */
        private double getDefaultThreshold() {
                return policiesRepository.findByIsActiveTrue()
                                .map(p -> p.getThresholdApproval().doubleValue())
                                .orElse(0.5);
        }

        /**
         * Genera preview de alertas basado en umbrales temporales.
         * Usa la ÚLTIMA predicción por cuenta.
         */
        public EarlyWarningsPreviewDTO getWarningsPreview(double thresholdProbability, int daysGrace) {
                log.info("Generando preview de alertas: umbral={}%, días={}", thresholdProbability, daysGrace);

                // Obtener las últimas predicciones por cuenta (OPTIMIZADO)
                List<DefaultPrediction> latestPredictions = getLatestPredictionsPerAccount();

                if (latestPredictions.isEmpty()) {
                        return new EarlyWarningsPreviewDTO(0, 0, new ArrayList<>());
                }

                // Convertir umbral de probabilidad de pago a probabilidad de default
                // Si umbral = 30% prob pago, entonces default_probability > 0.70
                double defaultThreshold = 1.0 - (thresholdProbability / 100.0);

                List<AlertaDTO> alertas = new ArrayList<>();
                String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

                // Set para rastrear cuentas únicas y evitar doble conteo
                Set<Long> cuentasUnicasGlobal = new HashSet<>();
                double dineroTotalGlobal = 0.0;

                // Alerta 1: Cuentas con riesgo crítico (prob pago < 15%)
                List<DefaultPrediction> criticos = latestPredictions.stream()
                                .filter(p -> p.getDefaultProbability() != null)
                                .filter(p -> p.getDefaultProbability().doubleValue() > 0.85)
                                .collect(Collectors.toList());

                if (!criticos.isEmpty()) {
                        double dinero = criticos.stream()
                                        .filter(p -> p.getEstimatedLoss() != null)
                                        .mapToDouble(p -> p.getEstimatedLoss().doubleValue())
                                        .sum();

                        // Agregar recordIds al set global
                        criticos.forEach(p -> {
                                try {
                                        cuentasUnicasGlobal
                                                        .add(p.getMonthlyHistory().getAccountDetails().getRecordId());
                                } catch (Exception e) {
                                        /* ignorar */ }
                        });
                        dineroTotalGlobal += dinero;

                        alertas.add(new AlertaDTO(
                                        "alert-critico",
                                        "critico",
                                        "Cuentas en Riesgo Crítico",
                                        criticos.size() + " cuentas tienen menos de 15% de probabilidad de pago",
                                        criticos.size(),
                                        dinero,
                                        "urgente",
                                        fechaActual,
                                        "Contacto inmediato y evaluación de reestructuración"));
                }

                // Alerta 2: Cuentas bajo el umbral configurado (excluyendo críticos)
                List<DefaultPrediction> bajoUmbral = latestPredictions.stream()
                                .filter(p -> p.getDefaultProbability() != null)
                                .filter(p -> p.getDefaultProbability().doubleValue() > defaultThreshold)
                                .filter(p -> p.getDefaultProbability().doubleValue() <= 0.85) // Excluir críticos
                                .collect(Collectors.toList());

                if (!bajoUmbral.isEmpty()) {
                        double dinero = bajoUmbral.stream()
                                        .filter(p -> p.getEstimatedLoss() != null)
                                        .mapToDouble(p -> p.getEstimatedLoss().doubleValue())
                                        .sum();

                        bajoUmbral.forEach(p -> {
                                try {
                                        cuentasUnicasGlobal
                                                        .add(p.getMonthlyHistory().getAccountDetails().getRecordId());
                                } catch (Exception e) {
                                        /* ignorar */ }
                        });
                        dineroTotalGlobal += dinero;

                        alertas.add(new AlertaDTO(
                                        "alert-umbral",
                                        "alto",
                                        "Cuentas Bajo Umbral de Riesgo",
                                        bajoUmbral.size() + " cuentas bajo el umbral de " + (int) thresholdProbability
                                                        + "% de probabilidad de pago",
                                        bajoUmbral.size(),
                                        dinero,
                                        "alta",
                                        fechaActual,
                                        "Revisión de casos y contacto preventivo"));
                }

                // Alerta 3: Cuentas de clientes jóvenes en riesgo (< 30 años)
                // Nota: Esta es informativa, no suma al total global para evitar doble conteo
                List<DefaultPrediction> jovenesRiesgo = latestPredictions.stream()
                                .filter(p -> p.getDefaultProbability() != null
                                                && p.getDefaultProbability().doubleValue() > getDefaultThreshold())
                                .filter(p -> {
                                        try {
                                                Customer customer = p.getMonthlyHistory().getAccountDetails()
                                                                .getCustomer();
                                                return customer != null && customer.getAge() != null
                                                                && customer.getAge() < 30;
                                        } catch (Exception e) {
                                                return false;
                                        }
                                })
                                .collect(Collectors.toList());

                if (!jovenesRiesgo.isEmpty()) {
                        double dinero = jovenesRiesgo.stream()
                                        .filter(p -> p.getEstimatedLoss() != null)
                                        .mapToDouble(p -> p.getEstimatedLoss().doubleValue())
                                        .sum();

                        alertas.add(new AlertaDTO(
                                        "alert-jovenes",
                                        "tendencia",
                                        "Cuentas de Clientes Jóvenes en Riesgo",
                                        jovenesRiesgo.size()
                                                        + " cuentas de clientes menores de 30 años presentan riesgo elevado",
                                        jovenesRiesgo.size(),
                                        dinero,
                                        "media",
                                        fechaActual,
                                        "Programa de educación financiera y seguimiento"));
                }

                // Alerta 4: Factor de riesgo común
                Map<String, Long> riskFactorCount = latestPredictions.stream()
                                .filter(p -> p.getMainRiskFactor() != null && !p.getMainRiskFactor().isEmpty())
                                .filter(p -> p.getDefaultProbability() != null
                                                && p.getDefaultProbability().doubleValue() > getDefaultThreshold())
                                .collect(Collectors.groupingBy(
                                                DefaultPrediction::getMainRiskFactor,
                                                Collectors.counting()));

                if (!riskFactorCount.isEmpty()) {
                        String topFactor = riskFactorCount.entrySet().stream()
                                        .max(Map.Entry.comparingByValue())
                                        .map(Map.Entry::getKey)
                                        .orElse("Desconocido");
                        long count = riskFactorCount.getOrDefault(topFactor, 0L);

                        if (count > 2) {
                                alertas.add(new AlertaDTO(
                                                "alert-factor",
                                                "tendencia",
                                                "Factor de Riesgo Dominante: " + topFactor,
                                                count + " cuentas en riesgo comparten este factor como principal causa",
                                                (int) count,
                                                0,
                                                "media",
                                                fechaActual,
                                                "Analizar políticas relacionadas con: " + topFactor));
                        }
                }

                // Usar conteo único de cuentas y dinero acumulado (sin doble conteo)
                long totalCuentas = cuentasUnicasGlobal.size();
                double totalDinero = dineroTotalGlobal;

                log.info("Preview generado: {} alertas, {} cuentas únicas, ${} en riesgo",
                                alertas.size(), totalCuentas, totalDinero);

                return new EarlyWarningsPreviewDTO(totalCuentas, totalDinero, alertas);
        }

        /**
         * Obtiene la última predicción por cada cuenta (record_id).
         * OPTIMIZADO con SQL nativo y window function para evitar O(n²).
         */
        @SuppressWarnings("unchecked")
        private List<DefaultPrediction> getLatestPredictionsPerAccount() {
                // Query nativa para obtener los IDs de las predicciones más recientes por
                // cuenta
                String nativeQuery = """
                                SELECT dp.id_prediction FROM (
                                    SELECT
                                        dp.id_prediction,
                                        ROW_NUMBER() OVER (
                                            PARTITION BY mh.record_id
                                            ORDER BY dp.date_prediction DESC
                                        ) as rn
                                    FROM default_prediction dp
                                    JOIN monthly_history mh ON mh.id_historial = dp.id_historial
                                ) dp
                                WHERE dp.rn = 1
                                """;

                List<Long> ids = entityManager.createNativeQuery(nativeQuery)
                                .getResultList();

                if (ids.isEmpty()) {
                        return new ArrayList<>();
                }

                // Cargar las entidades completas usando los IDs obtenidos
                String jpql = """
                                SELECT dp FROM DefaultPrediction dp
                                JOIN FETCH dp.monthlyHistory mh
                                JOIN FETCH mh.accountDetails ad
                                JOIN FETCH ad.customer c
                                WHERE dp.idPrediction IN :ids
                                """;

                return entityManager.createQuery(jpql, DefaultPrediction.class)
                                .setParameter("ids", ids)
                                .getResultList();
        }
}
