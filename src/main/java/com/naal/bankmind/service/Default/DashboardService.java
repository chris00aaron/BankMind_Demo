package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Response.DashboardMorosidadDTO;
import com.naal.bankmind.dto.Default.Response.DashboardMorosidadDTO.*;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.MonthlyHistory;
import com.naal.bankmind.entity.Default.DefaultPolicies;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.entity.Default.TrainingHistory;
import com.naal.bankmind.repository.Default.CustomerRepository;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para calcular y obtener datos del dashboard de morosidad.
 * OPTIMIZADO: Usa queries agregadas para evitar cargar todos los registros.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final DefaultPoliciesRepository policiesRepository;
    private final EntityManager entityManager;

    /**
     * Obtiene todos los datos del dashboard de morosidad.
     */
    public DashboardMorosidadDTO getDashboardData() {
        log.info("Calculando datos del dashboard de morosidad");

        // Obtener últimas predicciones por cuenta (una sola vez)
        List<DefaultPrediction> latestPredictions = getLatestPredictionsPerAccount();
        log.info("Predicciones únicas por cuenta: {}", latestPredictions.size());

        DashboardMorosidadDTO dashboard = new DashboardMorosidadDTO();

        dashboard.setMetricas(calcularMetricasResumen(latestPredictions));
        dashboard.setModelo(calcularMetricasModelo());
        dashboard.setDistribucionProbabilidad(calcularDistribucionProbabilidad(latestPredictions));
        dashboard.setSegmentacionRiesgo(calcularSegmentacionRiesgo(latestPredictions));
        dashboard.setTendenciaMensual(calcularTendenciaMensualOptimizado());
        dashboard.setClientesAltoRiesgo(obtenerClientesAltoRiesgo(latestPredictions));

        log.info("Dashboard calculado exitosamente");
        return dashboard;
    }

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
     * Obtiene la última predicción por cada cuenta (record_id).
     * OPTIMIZADO con SQL nativo y window function.
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

        List<Long> ids = entityManager.createNativeQuery(nativeQuery)
                .getResultList();

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

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

    /**
     * Calcula métricas resumen usando solo últimas predicciones.
     * NOTA: Cuenta CUENTAS, no clientes.
     */
    private MetricasResumen calcularMetricasResumen(List<DefaultPrediction> latestPredictions) {
        // Total de cuentas CON predicción (son las que estamos analizando)
        long totalCuentas = latestPredictions.size();

        // Cuentas que superan el umbral de riesgo
        long cuentasEnRiesgo = latestPredictions.stream()
                .filter(p -> p.getDefaultProbability() != null)
                .filter(p -> p.getDefaultProbability().doubleValue() > getDefaultThreshold())
                .count();

        // Suma de pérdida estimada solo de cuentas morosas (superan umbral)
        double dineroEnRiesgo = latestPredictions.stream()
                .filter(p -> Boolean.TRUE.equals(p.getDefaultPaymentNextMonth()))
                .filter(p -> p.getEstimatedLoss() != null)
                .mapToDouble(p -> p.getEstimatedLoss().doubleValue())
                .sum();

        // Suma de pérdida estimada de TODAS las cuentas
        double dineroEnRiesgoTotal = latestPredictions.stream()
                .filter(p -> p.getEstimatedLoss() != null)
                .mapToDouble(p -> p.getEstimatedLoss().doubleValue())
                .sum();

        // Tasa = cuentas en riesgo / total cuentas analizadas
        double tasaMorosidad = totalCuentas > 0
                ? (cuentasEnRiesgo * 100.0 / totalCuentas)
                : 0.0;

        return new MetricasResumen(
                totalCuentas,
                cuentasEnRiesgo,
                dineroEnRiesgo,
                dineroEnRiesgoTotal,
                Math.round(tasaMorosidad * 10.0) / 10.0);
    }

    /**
     * Obtiene métricas del modelo en producción.
     */
    private MetricasModelo calcularMetricasModelo() {
        String query = "SELECT th FROM TrainingHistory th WHERE th.inProduction = true ORDER BY th.trainingDate DESC";
        TypedQuery<TrainingHistory> typedQuery = entityManager.createQuery(query, TrainingHistory.class);
        typedQuery.setMaxResults(1);

        List<TrainingHistory> results = typedQuery.getResultList();

        if (results.isEmpty() || results.get(0).getMetricsResults() == null) {
            log.warn("No se encontró modelo en producción");
            return new MetricasModelo(0.0, 0.0, 0.0);
        }

        var metrics = results.get(0).getMetricsResults();
        return new MetricasModelo(
                metrics.getPrecision() != null ? metrics.getPrecision() * 100 : 0.0,
                metrics.getRecall() != null ? metrics.getRecall() * 100 : 0.0,
                metrics.getF1Score() != null ? metrics.getF1Score() * 100 : 0.0);
    }

    /**
     * Distribución por rangos de probabilidad.
     */
    private List<DistribucionProbabilidad> calcularDistribucionProbabilidad(List<DefaultPrediction> latestPredictions) {
        Map<String, Long> rangos = new LinkedHashMap<>();
        rangos.put("0-20%", 0L);
        rangos.put("20-40%", 0L);
        rangos.put("40-60%", 0L);
        rangos.put("60-80%", 0L);
        rangos.put("80-100%", 0L);

        for (DefaultPrediction pred : latestPredictions) {
            if (pred.getDefaultProbability() == null)
                continue;
            double probPago = (1.0 - pred.getDefaultProbability().doubleValue()) * 100;

            if (probPago < 20)
                rangos.merge("0-20%", 1L, Long::sum);
            else if (probPago < 40)
                rangos.merge("20-40%", 1L, Long::sum);
            else if (probPago < 60)
                rangos.merge("40-60%", 1L, Long::sum);
            else if (probPago < 80)
                rangos.merge("60-80%", 1L, Long::sum);
            else
                rangos.merge("80-100%", 1L, Long::sum);
        }

        return rangos.entrySet().stream()
                .map(e -> new DistribucionProbabilidad(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Segmentación por nivel de riesgo.
     */
    private List<SegmentacionRiesgo> calcularSegmentacionRiesgo(List<DefaultPrediction> latestPredictions) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        Map<String, Double> dinero = new LinkedHashMap<>();

        conteo.put("Crítico", 0L);
        conteo.put("Alto", 0L);
        conteo.put("Medio", 0L);
        conteo.put("Bajo", 0L);
        dinero.put("Crítico", 0.0);
        dinero.put("Alto", 0.0);
        dinero.put("Medio", 0.0);
        dinero.put("Bajo", 0.0);

        for (DefaultPrediction pred : latestPredictions) {
            if (pred.getDefaultProbability() == null)
                continue;
            double probPago = (1.0 - pred.getDefaultProbability().doubleValue()) * 100;
            String nivel = calcularNivelRiesgo(probPago);
            double loss = pred.getEstimatedLoss() != null ? pred.getEstimatedLoss().doubleValue() : 0.0;

            conteo.merge(nivel, 1L, Long::sum);
            dinero.merge(nivel, loss, Double::sum);
        }

        List<SegmentacionRiesgo> result = new ArrayList<>();
        for (String nivel : conteo.keySet()) {
            result.add(new SegmentacionRiesgo(nivel, conteo.get(nivel), dinero.get(nivel)));
        }
        return result;
    }

    /**
     * Tendencia mensual OPTIMIZADA con SQL agregado.
     * No carga todos los registros, solo las agregaciones.
     */
    @SuppressWarnings("unchecked")
    private List<TendenciaMensual> calcularTendenciaMensualOptimizado() {
        // Query agregada: calcula totales y morosos por mes directamente en SQL
        String nativeQuery = """
                WITH date_range AS (
                    SELECT MAX(monthly_period) as max_date,
                           MAX(monthly_period) - INTERVAL '5 months' as min_date
                    FROM monthly_history
                ),
                monthly_stats AS (
                    SELECT
                        TO_CHAR(mh.monthly_period, 'YYYY-MM') as mes,
                        TO_CHAR(mh.monthly_period, 'MON-YY') as mes_label,
                        COUNT(*) as total,
                        SUM(CASE WHEN mh.pay_x > 0 THEN 1 ELSE 0 END) as morosos
                    FROM monthly_history mh, date_range dr
                    WHERE mh.monthly_period >= dr.min_date AND mh.monthly_period <= dr.max_date
                    GROUP BY TO_CHAR(mh.monthly_period, 'YYYY-MM'), TO_CHAR(mh.monthly_period, 'MON-YY')
                    ORDER BY mes
                )
                SELECT mes, mes_label, total, morosos FROM monthly_stats
                """;

        List<Object[]> stats = entityManager.createNativeQuery(nativeQuery).getResultList();

        if (stats.isEmpty()) {
            log.warn("No hay datos de morosidad en monthly_history");
            return new ArrayList<>();
        }

        // Obtener el mes máximo para asignar la predicción
        String mesMaximo = (String) stats.get(stats.size() - 1)[0];

        // Calcular predicción promedio desde las últimas predicciones
        String predQuery = """
                SELECT AVG(dp.default_probability) * 100
                FROM default_prediction dp
                JOIN monthly_history mh ON mh.id_historial = dp.id_historial
                WHERE dp.id_prediction IN (
                    SELECT sub.id_prediction FROM (
                        SELECT dp2.id_prediction,
                               ROW_NUMBER() OVER (PARTITION BY mh2.record_id ORDER BY dp2.date_prediction DESC) as rn
                        FROM default_prediction dp2
                        JOIN monthly_history mh2 ON mh2.id_historial = dp2.id_historial
                    ) sub WHERE sub.rn = 1
                )
                """;

        Number predResult = (Number) entityManager.createNativeQuery(predQuery).getSingleResult();
        double prediccionPromedio = predResult != null ? predResult.doubleValue() : 0.0;

        log.info("Predicción promedio: {}%", prediccionPromedio);

        List<TendenciaMensual> result = new ArrayList<>();
        for (Object[] row : stats) {
            String mes = (String) row[0];
            String mesLabel = ((String) row[1]).toUpperCase();
            long total = ((Number) row[2]).longValue();
            long morosos = ((Number) row[3]).longValue();

            double morosidadPct = total > 0 ? (morosos * 100.0 / total) : 0.0;
            double prediccion = mes.equals(mesMaximo) ? prediccionPromedio : 0.0;

            log.info("Mes {}: total={}, morosos={}, morosidad={}%, prediccion={}%",
                    mesLabel, total, morosos, morosidadPct, prediccion);

            result.add(new TendenciaMensual(
                    mesLabel,
                    Math.round(morosidadPct * 10.0) / 10.0,
                    Math.round(prediccion * 10.0) / 10.0));
        }

        return result;
    }

    /**
     * Clientes de alto riesgo (sin N+1 para cuotas atrasadas).
     */
    private List<ClienteAltoRiesgo> obtenerClientesAltoRiesgo(List<DefaultPrediction> latestPredictions) {
        // Filtrar top 10 de alto riesgo
        List<DefaultPrediction> altoRiesgo = latestPredictions.stream()
                .filter(p -> p.getDefaultProbability() != null)
                .filter(p -> p.getDefaultProbability().doubleValue() > getDefaultThreshold())
                .sorted((a, b) -> b.getDefaultProbability().compareTo(a.getDefaultProbability()))
                .limit(10)
                .collect(Collectors.toList());

        if (altoRiesgo.isEmpty()) {
            return new ArrayList<>();
        }

        // Obtener record_ids para buscar cuotas atrasadas en una sola query
        List<Long> recordIds = altoRiesgo.stream()
                .map(p -> p.getMonthlyHistory().getAccountDetails().getRecordId())
                .collect(Collectors.toList());

        // Query única para obtener cuotas atrasadas de todos los clientes
        Map<Long, Integer> cuotasPorCuenta = obtenerCuotasAtrasadasBatch(recordIds);

        List<ClienteAltoRiesgo> result = new ArrayList<>();
        for (DefaultPrediction pred : altoRiesgo) {
            try {
                Customer customer = pred.getMonthlyHistory().getAccountDetails().getCustomer();
                MonthlyHistory mh = pred.getMonthlyHistory();
                Long recordId = mh.getAccountDetails().getRecordId();

                double probPago = (1.0 - pred.getDefaultProbability().doubleValue()) * 100;
                String nivelRiesgo = calcularNivelRiesgo(probPago);

                String nombre = (customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                        (customer.getSurname() != null ? customer.getSurname() : "");

                result.add(new ClienteAltoRiesgo(
                        recordId, // ID de la cuenta (recordId)
                        nombre.trim(),
                        Math.round(probPago * 10.0) / 10.0,
                        nivelRiesgo,
                        mh.getBillAmtX() != null ? mh.getBillAmtX().doubleValue() : 0.0,
                        cuotasPorCuenta.getOrDefault(recordId, 0)));
            } catch (Exception e) {
                log.warn("Error procesando cliente: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * Obtiene cuotas atrasadas para múltiples cuentas en UNA sola query.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> obtenerCuotasAtrasadasBatch(List<Long> recordIds) {
        if (recordIds.isEmpty())
            return new HashMap<>();

        String nativeQuery = """
                WITH ranked AS (
                    SELECT record_id, pay_x,
                           ROW_NUMBER() OVER (PARTITION BY record_id ORDER BY monthly_period DESC) as rn
                    FROM monthly_history
                    WHERE record_id IN :ids
                )
                SELECT record_id, SUM(CASE WHEN pay_x > 0 THEN 1 ELSE 0 END) as atrasadas
                FROM ranked
                WHERE rn <= 6
                GROUP BY record_id
                """;

        List<Object[]> results = entityManager.createNativeQuery(nativeQuery)
                .setParameter("ids", recordIds)
                .getResultList();

        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : results) {
            Long recordId = ((Number) row[0]).longValue();
            Integer atrasadas = ((Number) row[1]).intValue();
            map.put(recordId, atrasadas);
        }
        return map;
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
}
