package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Response.DashboardMorosidadDTO;
import com.naal.bankmind.dto.Default.Response.DashboardMorosidadDTO.*;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.entity.Default.MonthlyHistory;
import com.naal.bankmind.entity.Default.TrainingHistory;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
        dashboard.setDistribucionSBS(calcularDistribucionSBS(latestPredictions));

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
            double probImpago = pred.getDefaultProbability().doubleValue() * 100;

            if (probImpago < 20)
                rangos.merge("0-20%", 1L, (a, b) -> a + b);
            else if (probImpago < 40)
                rangos.merge("20-40%", 1L, (a, b) -> a + b);
            else if (probImpago < 60)
                rangos.merge("40-60%", 1L, (a, b) -> a + b);
            else if (probImpago < 80)
                rangos.merge("60-80%", 1L, (a, b) -> a + b);
            else
                rangos.merge("80-100%", 1L, (a, b) -> a + b);
        }

        return rangos.entrySet().stream()
                .map(e -> new DistribucionProbabilidad(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Segmentación por clasificación SBS REAL (basada en payX actual del cliente).
     * Escala: payX ≤ 0 → Normal | 1 → CPP | 2 → Deficiente | 3 → Dudoso | ≥ 4 →
     * Pérdida
     */
    private List<SegmentacionRiesgo> calcularSegmentacionRiesgo(List<DefaultPrediction> latestPredictions) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        Map<String, Double> dinero = new LinkedHashMap<>();

        conteo.put("Normal", 0L);
        conteo.put("CPP", 0L);
        conteo.put("Deficiente", 0L);
        conteo.put("Dudoso", 0L);
        conteo.put("Pérdida", 0L);
        dinero.put("Normal", 0.0);
        dinero.put("CPP", 0.0);
        dinero.put("Deficiente", 0.0);
        dinero.put("Dudoso", 0.0);
        dinero.put("Pérdida", 0.0);

        for (DefaultPrediction pred : latestPredictions) {
            // Clasificación REAL: derivada del payX del periodo asociado a esta predicción
            Integer payX = pred.getMonthlyHistory() != null ? pred.getMonthlyHistory().getPayX() : null;
            String catReal = derivarClasificacionSBSReal(payX);
            double loss = pred.getEstimatedLoss() != null ? pred.getEstimatedLoss().doubleValue() : 0.0;

            conteo.merge(catReal, 1L, (a, b) -> a + b);
            dinero.merge(catReal, loss, (a, b) -> a + b);
        }

        List<SegmentacionRiesgo> result = new ArrayList<>();
        for (String cat : conteo.keySet()) {
            result.add(new SegmentacionRiesgo(cat, conteo.get(cat), dinero.get(cat)));
        }
        return result;
    }

    /**
     * Convierte el valor payX en la clasificación SBS real correspondiente.
     * payX ≤ 0 → Normal | 1 → CPP | 2 → Deficiente | 3-4 → Dudoso | ≥ 5 → Pérdida
     */
    private String derivarClasificacionSBSReal(Integer payX) {
        if (payX == null || payX <= 0)
            return "Normal";
        if (payX == 1)
            return "CPP";
        if (payX == 2)
            return "Deficiente";
        if (payX <= 4)
            return "Dudoso";
        return "Pérdida";
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

        // Calcular Tasa Predicha de Morosidad (clientes sobre el umbral / total de clientes) para que coincida con el KPI "Tasa de Morosidad"
        String predQuery = """
                SELECT (SUM(CASE WHEN dp.default_probability > :threshold THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(*), 0)
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

        Number predResult = (Number) entityManager.createNativeQuery(predQuery)
                .setParameter("threshold", getDefaultThreshold())
                .getSingleResult();
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
                String clasificacionSBS = pred.getDefaultCategory() != null ? pred.getDefaultCategory()
                        : "Sin clasificar";

                String nombre = (customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                        (customer.getSurname() != null ? customer.getSurname() : "");

                result.add(new ClienteAltoRiesgo(
                        recordId, // ID de la cuenta (recordId)
                        nombre.trim(),
                        Math.round(probPago * 10.0) / 10.0,
                        clasificacionSBS,
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

    // calcularNivelRiesgo eliminado — se usa clasificaciónSBS unificada

    /**
     * Distribución por categoría SBS predicha.
     */
    private List<DistribucionSBS> calcularDistribucionSBS(List<DefaultPrediction> latestPredictions) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        conteo.put("Normal", 0L);
        conteo.put("CPP", 0L);
        conteo.put("Deficiente", 0L);
        conteo.put("Dudoso", 0L);
        conteo.put("Pérdida", 0L);

        for (DefaultPrediction pred : latestPredictions) {
            String cat = pred.getDefaultCategory();
            if (cat == null || cat.isBlank())
                cat = "Sin clasificar";
            conteo.merge(cat, 1L, (a, b) -> a + b);
        }

        return conteo.entrySet().stream()
                .map(e -> new DistribucionSBS(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los clientes paginados y filtrados.
     */
    public Page<ClienteAltoRiesgo> getDashboardClientsPaginated(int page, int size, String nombre,
            String clasificacionSBS, String sortBy, String sortDir, String educacion,
            Integer edadMin, Integer edadMax) {
        List<DefaultPrediction> latestPredictions = getLatestPredictionsPerAccount();

        List<DefaultPrediction> filtered = new ArrayList<>();
        for (DefaultPrediction pred : latestPredictions) {
            Customer customer = pred.getMonthlyHistory().getAccountDetails().getCustomer();

            boolean matchSbs = true;
            if (clasificacionSBS != null && !clasificacionSBS.isBlank()) {
                String cat = pred.getMonthlyHistory().getAccountDetails().getSbsCategoryReal();
                if (cat == null)
                    cat = "Sin clasificar";
                matchSbs = cat.equalsIgnoreCase(clasificacionSBS.trim());
            }

            String fullName = (customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                    (customer.getSurname() != null ? customer.getSurname() : "");
            fullName = fullName.trim();
            boolean matchNombre = true;
            if (nombre != null && !nombre.isBlank()) {
                matchNombre = fullName.toLowerCase().contains(nombre.trim().toLowerCase());
            }

            boolean matchEducacion = true;
            if (educacion != null && !educacion.isBlank()) {
                Integer idEdu = customer.getEducation() != null ? customer.getEducation().getIdEducation() : null;
                String mappedEdu = "Otro";
                if (idEdu != null) {
                    mappedEdu = switch (idEdu) {
                        case 1 -> "Postgrado";
                        case 2 -> "Universidad"; // Alineado con el combobox 'Universidad' en React
                        case 3 -> "Secundaria";
                        case 4 -> "Primaria";
                        default -> "Otro";
                    };
                }

                // En el combobox de React las opciones son: Universidad, Posgrado,
                // Preparatoria, Otro
                // Mapearemos "Posgrado" de React a "Postgrado" y "Preparatoria/Otro" a
                // "Secundaria/Otro"
                String eduFiltro = educacion.trim();
                if (eduFiltro.equalsIgnoreCase("Posgrado"))
                    eduFiltro = "Postgrado";
                if (eduFiltro.equalsIgnoreCase("Preparatoria"))
                    eduFiltro = "Secundaria";
                if (eduFiltro.equalsIgnoreCase("Otro") && mappedEdu.equals("Primaria"))
                    eduFiltro = "Primaria"; // Match all others to 'Otro'

                matchEducacion = mappedEdu.equalsIgnoreCase(eduFiltro) ||
                        (eduFiltro.equalsIgnoreCase("Otro")
                                && (mappedEdu.equals("Otro") || mappedEdu.equals("Secundaria")));
            }

            boolean matchEdad = true;
            Integer age = customer.getAge();
            if (edadMin != null)
                matchEdad = age != null && age >= edadMin;
            if (edadMax != null && matchEdad)
                matchEdad = age != null && age <= edadMax;

            if (matchSbs && matchNombre && matchEducacion && matchEdad) {
                filtered.add(pred);
            }
        }

        // Ordenamiento dinámico
        Comparator<DefaultPrediction> comparator;
        switch (sortBy != null ? sortBy : "probabilidadPago") {
            case "montoCuota":
                comparator = Comparator.comparing(
                        p -> p.getMonthlyHistory().getBillAmtX() != null
                                ? p.getMonthlyHistory().getBillAmtX().doubleValue()
                                : 0.0);
                break;
            case "recordId":
                comparator = Comparator.comparing(
                        p -> p.getMonthlyHistory().getAccountDetails().getRecordId());
                break;
            case "cuotasAtrasadas":
                comparator = Comparator.comparing(
                        p -> p.getMonthlyHistory().getPayX() != null ? p.getMonthlyHistory().getPayX() : 0);
                break;
            default: // probabilidadPago
                comparator = Comparator.comparing(
                        p -> p.getDefaultProbability() != null ? p.getDefaultProbability().doubleValue() : 0.0);
                break;
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator);

        int totalElements = filtered.size();
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<DefaultPrediction> pagePredictions = filtered.subList(start, end);

        List<Long> recordIdsToFetch = pagePredictions.stream()
                .map(p -> p.getMonthlyHistory().getAccountDetails().getRecordId())
                .collect(Collectors.toList());

        Map<Long, Integer> cuotasPorCuenta = obtenerCuotasAtrasadasBatch(recordIdsToFetch);

        List<ClienteAltoRiesgo> resultClients = new ArrayList<>();
        for (DefaultPrediction pred : pagePredictions) {
            Customer customer = pred.getMonthlyHistory().getAccountDetails().getCustomer();
            MonthlyHistory mh = pred.getMonthlyHistory();
            Long recordId = mh.getAccountDetails().getRecordId();

            double probPago = (1.0 - pred.getDefaultProbability().doubleValue()) * 100;
            String clasificacion = mh.getAccountDetails().getSbsCategoryReal() != null
                    ? mh.getAccountDetails().getSbsCategoryReal()
                    : "Sin clasificar";
            String fullName = (customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                    (customer.getSurname() != null ? customer.getSurname() : "");

            resultClients.add(new ClienteAltoRiesgo(
                    recordId,
                    fullName.trim(),
                    Math.round(probPago * 10.0) / 10.0,
                    clasificacion,
                    mh.getBillAmtX() != null ? mh.getBillAmtX().doubleValue() : 0.0,
                    cuotasPorCuenta.getOrDefault(recordId, 0)));
        }

        return new PageImpl<>(resultClients, PageRequest.of(page, size), totalElements);
    }
}
