package com.naal.bankmind.service.Default;

import com.naal.bankmind.entity.Default.ModelMonitoringLog;
import com.naal.bankmind.entity.Default.TrainingHistory;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.entity.MonthlyHistory;
import com.naal.bankmind.repository.Default.ModelMonitoringLogRepository;
import com.naal.bankmind.repository.Default.TrainingHistoryRepository;
import com.naal.bankmind.repository.Default.DefaultPredictionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final EntityManager entityManager;
    private final ModelMonitoringLogRepository monitoringLogRepository;
    private final TrainingHistoryRepository trainingHistoryRepository;
    private final DefaultPredictionRepository defaultPredictionRepository;
    private final SelfTrainingService selfTrainingService;

    // Features críticas para monitorear drift
    private static final List<String> FEATURES_TO_MONITOR = List.of(
            "PAY_0", "PAY_2", "PAY_3", "LIMIT_BAL", "BILL_AMT1", "UTILIZATION_RATE");

    /**
     * TAREA DIARIA (1:00 AM)
     * Verifica si hubo data drift en las features clave comparando con el baseline.
     * Si drift > 0.25 por 3 días consecutivos -> Trigger Auto-Retraining.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void checkDailyDrift() {
        log.info("🕵️ Iniciando monitoreo diario de Data Drift...");

        // 1. Obtener el modelo activo (el último entrenado exitosamente con baseline)
        Optional<TrainingHistory> activeModelOpt = trainingHistoryRepository.findAll().stream()
                .filter(th -> th.getBaselineDistributions() != null)
                .sorted(Comparator.comparing(TrainingHistory::getIdTrainingHistory).reversed())
                .findFirst();

        if (activeModelOpt.isEmpty()) {
            log.warn("⚠️ No hay modelo activo con baseline distributions. Saltando monitoreo.");
            return;
        }

        TrainingHistory activeModel = activeModelOpt.get();
        Map<String, Object> baseline = activeModel.getBaselineDistributions();

        // 2. Extraer data del día anterior (simulado consultando la vista reciente)
        // En prod real: consultaríamos solo los registros insertados ayer.
        // Aquí usamos la vista completa filtrada por fecha reciente para simular
        // 'current data'.

        List<Map<String, Object>> currentData = fetchCurrentDataSamples();

        if (currentData.isEmpty()) {
            log.warn("⚠️ No hay data reciente para monitorear.");
            return;
        }

        // 3. Calcular PSI para cada feature
        Map<String, Object> psiResults = new HashMap<>();
        double totalPsi = 0;
        int featuresCount = 0;

        for (String feature : FEATURES_TO_MONITOR) {
            if (baseline.containsKey(feature)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> baseDist = (Map<String, Object>) baseline.get(feature);
                double psi = calculatePsiForFeature(feature, baseDist, currentData);

                psiResults.put(feature, psi);
                totalPsi += psi;
                featuresCount++;
            }
        }

        double avgPsi = featuresCount > 0 ? totalPsi / featuresCount : 0;
        log.info("📊 PSI Promedio del día: {}", String.format("%.4f", avgPsi));

        // 4. Evaluar reglas de Trigger
        boolean significantDrift = avgPsi > 0.25;

        // Obtener historial previo para contar días consecutivos
        int consecutiveDays = 0;
        Optional<ModelMonitoringLog> lastLog = monitoringLogRepository.findTopByOrderByIdMonitoringDesc();

        if (lastLog.isPresent() && lastLog.get().getDriftDetected()) {
            consecutiveDays = lastLog.get().getConsecutiveDaysDrift();
        }

        if (significantDrift) {
            consecutiveDays++;
            log.warn("🚨 Drift detectado! Días consecutivos: {}", consecutiveDays);
        } else {
            consecutiveDays = 0; // Reset si hoy está ok
        }

        // 5. Guardar Log
        ModelMonitoringLog logEntry = new ModelMonitoringLog();
        logEntry.setMonitoringDate(LocalDate.now());
        logEntry.setIdTrainingModel(activeModel.getIdTrainingHistory());
        logEntry.setPsiFeatures(psiResults);
        logEntry.setDriftDetected(significantDrift);
        logEntry.setConsecutiveDaysDrift(consecutiveDays);
        monitoringLogRepository.save(logEntry);

        // 6. Trigger Auto-Retraining si corresponde
        if (consecutiveDays >= 3) {
            log.error("🚀 ALERTA CRÍTICA: Drift sostenido por 3 días. Iniciando Auto-Retraining...");
            selfTrainingService.ejecutarRetraining(30); // 30 trials
        }
    }

    /**
     * TAREA MENSUAL (Día 1 a las 2:00 AM)
     * Valida el desempeño del modelo con labels reales confirmados.
     * Compara predicciones de hace 2 meses vs lo que realmente pasó.
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void validateMonthlyPerformance() {
        log.info("📅 Iniciando validación mensual de Performance...");

        // Evaluar el mes antepasado (ej: si hoy es 1 de Mayo, evaluamos Marzo,
        // porque en Abril terminaron de pagar o vencerse las deudas de Marzo)
        LocalDateTime startValidationInfo = LocalDate.now().minusMonths(2).atStartOfDay();
        LocalDateTime endValidationInfo = LocalDate.now().minusMonths(1).atStartOfDay();

        // Obtener el modelo que estaba activo en ese periodo
        // (Simplificación: tomamos el modelo vinculado a las predicciones)
        // Agrupamos validación por modelo ID si hubo cambios de modelo en el mes

        // 1. Obtener predicciones históricas
        // Necesitamos saber qué modelo hizo esas predicciones.
        // Asumimos que iteramos sobre los modelos activos en ese rango.
        // O más simple: validamos TODO lo predicho en ese mes agrupado por modelo.

        List<Long> modelIds = entityManager.createQuery(
                "SELECT DISTINCT p.idProductionModel.idModel FROM DefaultPrediction p " +
                        "WHERE p.datePrediction BETWEEN :start AND :end",
                Long.class)
                .setParameter("start", startValidationInfo)
                .setParameter("end", endValidationInfo)
                .getResultList();

        for (Long modelId : modelIds) {
            validateModelForPeriod(modelId, startValidationInfo, endValidationInfo);
        }
    }

    private void validateModelForPeriod(Long modelId, LocalDateTime start, LocalDateTime end) {
        log.info("🔍 Validando Modelo ID {} para el periodo {} - {}", modelId, start, end);

        List<DefaultPrediction> predictions = defaultPredictionRepository.findPredictionsForValidation(start, end,
                modelId);

        if (predictions.isEmpty()) {
            log.warn("⚠️ No se encontraron predicciones para validar modelo {}", modelId);
            return;
        }

        List<Double> yScores = new ArrayList<>();
        List<Integer> yTrue = new ArrayList<>();

        for (DefaultPrediction p : predictions) {
            MonthlyHistory mh = p.getMonthlyHistory();
            if (mh == null)
                continue;

            // Determinar Label Real (1 = Moroso, 0 = No Moroso)
            // Logica: Si pagó después de fecha_vencimiento + dias_gracia -> Moroso
            int label = 0;
            Integer gracePeriod = p.getIdPolicy() != null ? p.getIdPolicy().getDaysGraceDefault() : 8; // default 8

            LocalDate expiration = mh.getExpirationDate();
            LocalDate payment = mh.getActualPaymentDate();

            if (payment == null) {
                // No pagó -> Moroso
                label = 1;
            } else if (expiration != null) {
                LocalDate deadline = expiration.plusDays(gracePeriod);
                if (payment.isAfter(deadline)) {
                    label = 1;
                }
            }

            yTrue.add(label);
            yScores.add(p.getDefaultProbability().doubleValue());
        }

        if (yTrue.isEmpty())
            return;

        // Calcular Métricas
        double aucReal = calculateAuc(yTrue, yScores);
        double ksReal = calculateKs(yTrue, yScores);

        double predictedRate = predictions.stream()
                .filter(DefaultPrediction::getDefaultPaymentNextMonth)
                .count() / (double) predictions.size();

        double actualRate = yTrue.stream().filter(l -> l == 1).count() / (double) yTrue.size();

        log.info("📉 Resultados Reales: AUC={} | KS={} | Tasa Predicha={}% | Tasa Real={}%",
                String.format("%.4f", aucReal), String.format("%.4f", ksReal),
                String.format("%.2f", predictedRate * 100), String.format("%.2f", actualRate * 100));

        // Comparar con métricas de entrenamiento (Baseline)
        // Buscamos el TrainingHistory asociado a este ProductionModel
        // NOTA: Asumimos que idProductionModel mapea a un idTrainingHistory o similar.
        // Si no tenemos enlace directo fácil, usamos el último training history previo
        // a la fecha.
        // Aquí simplificaremos buscando el TrainingHistory por ID si coinciden, o
        // consultando repo.
        Optional<TrainingHistory> historyOpt = trainingHistoryRepository.findById(modelId); // Asumiendo ID compartido o
                                                                                            // mapeo

        boolean triggerRetraining = false;

        if (historyOpt.isPresent()) {
            double aucTrain = historyOpt.get().getMetricsResults().getAucRoc();
            // Regla: Caída de AUC > 5%
            if ((aucTrain - aucReal) > 0.05) {
                log.warn("🚨 DEGRADACIÓN DETECTADA: Caída de AUC ({} -> {})", aucTrain, aucReal);
                triggerRetraining = true;
            }
            // Regla: Caída de KS > 10%
            double ksTrain = historyOpt.get().getMetricsResults().getKsStatistic();
            if ((ksTrain - ksReal) > 0.10) {
                log.warn("🚨 DEGRADACIÓN DETECTADA: Caída de KS ({} -> {})", ksTrain, ksReal);
                triggerRetraining = true;
            }
        }

        // Guardar Log (Mensual)
        ModelMonitoringLog monitoringEntry = new ModelMonitoringLog();
        monitoringEntry.setMonitoringDate(LocalDate.now());
        monitoringEntry.setIdTrainingModel(modelId);
        monitoringEntry.setValidationStatus("VALIDATED");
        monitoringEntry.setAucRocReal(aucReal);
        monitoringEntry.setKsReal(ksReal);
        monitoringEntry.setPredictedDefaultRate(predictedRate);
        monitoringEntry.setActualDefaultRate(actualRate);
        monitoringLogRepository.save(monitoringEntry);

        if (triggerRetraining) {
            log.error("🚀 ALERTA MENSUAL: Degradación de performance confirmada. Iniciando Auto-Retraining...");
            selfTrainingService.ejecutarRetraining(50); // Más exhaustivo
        }
    }

    // --- Métricas Manuales (Simple Implementation) ---

    private double calculateAuc(List<Integer> yTrue, List<Double> yScores) {
        // Implementación Mann-Whitney U
        // 1. Separar scores de positivos y negativos
        List<Double> posScores = new ArrayList<>();
        List<Double> negScores = new ArrayList<>();

        for (int i = 0; i < yTrue.size(); i++) {
            if (yTrue.get(i) == 1)
                posScores.add(yScores.get(i));
            else
                negScores.add(yScores.get(i));
        }

        if (posScores.isEmpty() || negScores.isEmpty())
            return 0.0;

        double validPairs = 0;
        for (Double pos : posScores) {
            for (Double neg : negScores) {
                if (pos > neg)
                    validPairs += 1.0;
                else if (pos.equals(neg))
                    validPairs += 0.5;
            }
        }

        return validPairs / (posScores.size() * negScores.size());
    }

    private double calculateKs(List<Integer> yTrue, List<Double> yScores) {
        // KS = max |CDF_pos - CDF_neg|
        // 1. Crear pares (score, label) y ordenar descendente por score
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < yTrue.size(); i++)
            pairs.add(new Pair(yScores.get(i), yTrue.get(i)));

        pairs.sort((a, b) -> Double.compare(b.score, a.score)); // Descendente

        double maxKs = 0.0;
        double cumPos = 0;
        double cumNeg = 0;

        long totalPos = yTrue.stream().filter(x -> x == 1).count();
        long totalNeg = yTrue.size() - totalPos;

        if (totalPos == 0 || totalNeg == 0)
            return 0.0;

        for (Pair p : pairs) {
            if (p.label == 1)
                cumPos++;
            else
                cumNeg++;

            double tpr = cumPos / totalPos;
            double fpr = cumNeg / totalNeg;

            double diff = Math.abs(tpr - fpr);
            if (diff > maxKs)
                maxKs = diff;
        }

        return maxKs;
    }

    private static class Pair {
        double score;
        int label;

        Pair(double s, int l) {
            this.score = s;
            this.label = l;
        }
    }

    private List<Map<String, Object>> fetchCurrentDataSamples() {
        // Simulación: Traer 1000 registros aleatorios recientes de la vista
        String sql = "SELECT limit_bal, pay_0, pay_2, pay_3, bill_amt1, " +
                "CASE WHEN limit_bal > 0 THEN bill_amt1/limit_bal ELSE 0 END as utilization_rate " +
                "FROM vw_training_dataset_morosidad " +
                "ORDER BY random() LIMIT 1000";

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("LIMIT_BAL", toDouble(row[0]));
            map.put("PAY_0", toDouble(row[1])); // PAY_X suelen ser enteros pero tratamos como double para binning
            map.put("PAY_2", toDouble(row[2]));
            map.put("PAY_3", toDouble(row[3]));
            map.put("BILL_AMT1", toDouble(row[4]));
            map.put("UTILIZATION_RATE", toDouble(row[5]));
            result.add(map);
        }
        return result;
    }

    private double calculatePsiForFeature(String feature, Map<String, Object> baseDist,
            List<Map<String, Object>> currentData) {
        try {
            List<Double> currentValues = currentData.stream()
                    .map(m -> toDouble(m.get(feature)))
                    .collect(Collectors.toList());

            if (currentValues.isEmpty())
                return 0.0;

            List<Double> baseProbs;
            List<Double> currentProbs;

            String type = (String) baseDist.get("type");

            if ("categorical".equals(type)) {
                // Para categóricas (PAY_X), usamos los valores exactos definidos en baseline
                @SuppressWarnings("unchecked")
                List<Double> values = castToListDouble(baseDist.get("values"));
                @SuppressWarnings("unchecked")
                List<Double> bProbs = castToListDouble(baseDist.get("probs"));
                baseProbs = bProbs;

                // Calcular probs actuales para los mismos valores
                currentProbs = new ArrayList<>();
                for (Double val : values) {
                    long count = currentValues.stream().filter(v -> Objects.equals(v, val)).count();
                    // Smoothing pequeño para evitar division por cero
                    double prob = (count + 0.001) / (currentValues.size() + 0.001 * values.size());
                    currentProbs.add(prob);
                }

            } else {
                // Para continuas (BILL_AMT, etc), usamos los bins definidos en baseline
                @SuppressWarnings("unchecked")
                List<Double> bins = castToListDouble(baseDist.get("bins"));
                @SuppressWarnings("unchecked")
                List<Double> bProbs = castToListDouble(baseDist.get("probs"));
                baseProbs = bProbs;

                // Calcular histograma actual usando los MISMOS bins
                long[] counts = new long[bins.size() - 1];
                for (Double val : currentValues) {
                    for (int i = 0; i < bins.size() - 1; i++) {
                        if (val >= bins.get(i) && val < bins.get(i + 1)) {
                            counts[i]++;
                            break;
                        }
                    }
                    // Edge case: valor maximo exacto
                    if (Objects.equals(val, bins.get(bins.size() - 1))) {
                        counts[bins.size() - 2]++;
                    }
                }

                currentProbs = new ArrayList<>();
                for (long c : counts) {
                    double prob = (c + 0.001) / (currentValues.size() + 0.001 * counts.length);
                    currentProbs.add(prob);
                }
            }

            // Fórmula PSI: sum( (actual% - expected%) * ln(actual% / expected%) )
            double psi = 0.0;
            for (int i = 0; i < baseProbs.size(); i++) {
                double actual = currentProbs.get(i);
                double expected = baseProbs.get(i);
                if (expected < 0.0001)
                    expected = 0.0001; // Evitar division por cero

                psi += (actual - expected) * Math.log(actual / expected);
            }

            return psi;

        } catch (Exception e) {
            log.error("Error calculando PSI para {}: {}", feature, e.getMessage());
            return 0.0;
        }
    }

    private Double toDouble(Object obj) {
        if (obj == null)
            return 0.0;
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Double> castToListDouble(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return list.stream().map(this::toDouble).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
