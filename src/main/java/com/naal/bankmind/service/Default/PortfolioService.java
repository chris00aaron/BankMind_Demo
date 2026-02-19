package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Response.PortfolioResponseDTO;
import com.naal.bankmind.dto.Default.Response.PortfolioResponseDTO.*;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.Default.DefaultPolicies;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para la vista de gestión de cartera en riesgo.
 * Provee lista de cuentas con predicción y métricas resumen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final DefaultPoliciesRepository policiesRepository;
    private final EntityManager entityManager;

    /**
     * Obtiene las cuentas en riesgo con filtros opcionales.
     *
     * @param nivelesRiesgo      Lista de niveles (Crítico, Alto, Medio, Bajo), null
     *                           = todos
     * @param clasificacionesSBS Lista de categorías SBS, null = todas
     * @param probMin            Probabilidad de pago mínima (0-100), null = 0
     * @param probMax            Probabilidad de pago máxima (0-100), null = 100
     */
    public PortfolioResponseDTO getRiskAccounts(
            List<String> nivelesRiesgo,
            List<String> clasificacionesSBS,
            Double probMin,
            Double probMax) {

        log.info("Obteniendo cuentas de cartera: riesgo={}, sbs={}, prob=[{}-{}]",
                nivelesRiesgo, clasificacionesSBS, probMin, probMax);

        List<DefaultPrediction> latestPredictions = getLatestPredictionsPerAccount();

        if (latestPredictions.isEmpty()) {
            return new PortfolioResponseDTO(
                    new PortfolioResumen(0, 0, new LinkedHashMap<>(), new LinkedHashMap<>()),
                    new ArrayList<>());
        }

        // Obtener política activa para clasificación SBS
        DefaultPolicies activePolicy = policiesRepository.findByIsActiveTrue().orElse(null);

        // Mapear predicciones a DTOs con todos los datos
        List<PortfolioCuenta> todasLasCuentas = latestPredictions.stream()
                .filter(p -> p.getDefaultProbability() != null)
                .map(p -> mapToCuenta(p, activePolicy))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Calcular resumen ANTES de filtrar (resumen del total)
        PortfolioResumen resumenTotal = calcularResumen(todasLasCuentas);

        // Aplicar filtros
        List<PortfolioCuenta> cuentasFiltradas = todasLasCuentas;

        if (nivelesRiesgo != null && !nivelesRiesgo.isEmpty()) {
            cuentasFiltradas = cuentasFiltradas.stream()
                    .filter(c -> nivelesRiesgo.contains(c.getNivelRiesgo()))
                    .collect(Collectors.toList());
        }

        if (clasificacionesSBS != null && !clasificacionesSBS.isEmpty()) {
            cuentasFiltradas = cuentasFiltradas.stream()
                    .filter(c -> clasificacionesSBS.contains(c.getClasificacionSBS()))
                    .collect(Collectors.toList());
        }

        double minProb = probMin != null ? probMin : 0;
        double maxProb = probMax != null ? probMax : 100;
        cuentasFiltradas = cuentasFiltradas.stream()
                .filter(c -> c.getProbabilidadPago() >= minProb && c.getProbabilidadPago() <= maxProb)
                .collect(Collectors.toList());

        // Ordenar por probabilidad de pago (mayor riesgo primero)
        cuentasFiltradas.sort(Comparator.comparingDouble(PortfolioCuenta::getProbabilidadPago));

        log.info("Cartera: {} cuentas totales, {} después de filtros", todasLasCuentas.size(), cuentasFiltradas.size());

        return new PortfolioResponseDTO(resumenTotal, cuentasFiltradas);
    }

    /**
     * Mapea una predicción a un DTO de cuenta de cartera.
     */
    private PortfolioCuenta mapToCuenta(DefaultPrediction pred, DefaultPolicies policy) {
        try {
            Customer customer = pred.getMonthlyHistory().getAccountDetails().getCustomer();
            Long recordId = pred.getMonthlyHistory().getAccountDetails().getRecordId();

            double probPago = (1.0 - pred.getDefaultProbability().doubleValue()) * 100;
            probPago = Math.round(probPago * 10.0) / 10.0;

            String nombre = ((customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                    (customer.getSurname() != null ? customer.getSurname() : "")).trim();

            String educacion = mapEducation(
                    customer.getEducation() != null ? customer.getEducation().getIdEducation() : 4);
            String estadoCivil = mapMarriage(
                    customer.getMarriage() != null ? customer.getMarriage().getIdMarriage() : 3);

            String nivelRiesgo = calcularNivelRiesgo(probPago);
            String clasificacionSBS = calcularClasificacionSBS(pred.getDefaultProbability().doubleValue(), policy);

            double estimatedLoss = pred.getEstimatedLoss() != null ? pred.getEstimatedLoss().doubleValue() : 0.0;

            String mainRiskFactor = pred.getMainRiskFactor();
            if (mainRiskFactor != null &&
                    (mainRiskFactor.equalsIgnoreCase("Batch") || mainRiskFactor.equalsIgnoreCase("Unknown"))) {
                mainRiskFactor = "—"; // No disponible
            }

            String fechaPrediccion = pred.getDatePrediction() != null
                    ? pred.getDatePrediction().format(DateTimeFormatter.ISO_DATE_TIME)
                    : "";

            return new PortfolioCuenta(
                    recordId, nombre, customer.getAge(), educacion, estadoCivil,
                    probPago, nivelRiesgo, clasificacionSBS, estimatedLoss,
                    mainRiskFactor, fechaPrediccion);
        } catch (Exception e) {
            log.warn("Error mapeando predicción {}: {}", pred.getIdPrediction(), e.getMessage());
            return null;
        }
    }

    /**
     * Calcula métricas resumen de la cartera.
     */
    private PortfolioResumen calcularResumen(List<PortfolioCuenta> cuentas) {
        long totalCuentas = cuentas.size();
        double exposicionTotal = cuentas.stream().mapToDouble(PortfolioCuenta::getEstimatedLoss).sum();

        // Distribución SBS
        Map<String, Long> distribucionSBS = new LinkedHashMap<>();
        distribucionSBS.put("Normal", 0L);
        distribucionSBS.put("CPP", 0L);
        distribucionSBS.put("Deficiente", 0L);
        distribucionSBS.put("Dudoso", 0L);
        distribucionSBS.put("Pérdida", 0L);

        for (PortfolioCuenta c : cuentas) {
            distribucionSBS.merge(c.getClasificacionSBS(), 1L, Long::sum);
        }

        // Distribución riesgo
        Map<String, Long> distribucionRiesgo = new LinkedHashMap<>();
        distribucionRiesgo.put("Crítico", 0L);
        distribucionRiesgo.put("Alto", 0L);
        distribucionRiesgo.put("Medio", 0L);
        distribucionRiesgo.put("Bajo", 0L);

        for (PortfolioCuenta c : cuentas) {
            distribucionRiesgo.merge(c.getNivelRiesgo(), 1L, Long::sum);
        }

        return new PortfolioResumen(totalCuentas, exposicionTotal, distribucionSBS, distribucionRiesgo);
    }

    /**
     * Obtiene la última predicción por cada cuenta.
     * Reutiliza la misma lógica optimizada de DashboardService.
     */
    @SuppressWarnings("unchecked")
    private List<DefaultPrediction> getLatestPredictionsPerAccount() {
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

        List<Long> ids = entityManager.createNativeQuery(nativeQuery).getResultList();

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = """
                SELECT dp FROM DefaultPrediction dp
                JOIN FETCH dp.monthlyHistory mh
                JOIN FETCH mh.accountDetails ad
                JOIN FETCH ad.customer c
                LEFT JOIN FETCH c.education
                LEFT JOIN FETCH c.marriage
                WHERE dp.idPrediction IN :ids
                """;

        return entityManager.createQuery(jpql, DefaultPrediction.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    private String calcularNivelRiesgo(double probabilidadPago) {
        if (probabilidadPago < 25)
            return "Crítico";
        if (probabilidadPago < 50)
            return "Alto";
        if (probabilidadPago < 75)
            return "Medio";
        return "Bajo";
    }

    private String calcularClasificacionSBS(double probabilidadDefault, DefaultPolicies policy) {
        if (policy == null || policy.getSbsClassificationMatrix() == null) {
            if (probabilidadDefault <= 0.05)
                return "Normal";
            if (probabilidadDefault <= 0.25)
                return "CPP";
            if (probabilidadDefault <= 0.60)
                return "Deficiente";
            if (probabilidadDefault <= 0.90)
                return "Dudoso";
            return "Pérdida";
        }

        for (var rule : policy.getSbsClassificationMatrix()) {
            if (probabilidadDefault >= rule.getMin() && probabilidadDefault < rule.getMax()) {
                return rule.getCategoria();
            }
        }
        return "Pérdida";
    }

    private String mapEducation(Integer idEducation) {
        return switch (idEducation) {
            case 1 -> "Postgrado";
            case 2 -> "Universitaria";
            case 3 -> "Secundaria";
            case 4 -> "Primaria";
            default -> "Otro";
        };
    }

    private String mapMarriage(Integer idMarriage) {
        return switch (idMarriage) {
            case 1 -> "Casado";
            case 2 -> "Soltero";
            case 3 -> "Divorciado";
            default -> "Otro";
        };
    }
}
