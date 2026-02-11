package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Request.TrainingSampleDTO;
import com.naal.bankmind.dto.Default.Request.TrainingRequestDTO;
import com.naal.bankmind.dto.Default.Response.TrainingResponseDTO;
import com.naal.bankmind.entity.Default.DatasetInfo;
import com.naal.bankmind.entity.Default.TrainingHistory;
import com.naal.bankmind.repository.Default.DatasetInfoRepository;
import com.naal.bankmind.repository.Default.TrainingHistoryRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio de orquestación para el auto-retraining del modelo de morosidad.
 *
 * Flujo:
 * 1. Refrescar la vista materializada
 * 2. Consultar el dataset desde la vista
 * 3. Enviar a la API Python para entrenar
 * 4. Guardar resultados en training_history y dataset_info
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfTrainingService {

    private final EntityManager entityManager;
    private final TrainingHistoryRepository trainingHistoryRepository;
    private final DatasetInfoRepository datasetInfoRepository;

    @Value("${self-training.api.base-url:http://localhost:8001}")
    private String selfTrainingApiUrl;

    @Value("${self-training.api.timeout:300000}")
    private int apiTimeout;

    /**
     * Ejecuta el pipeline completo de auto-retraining.
     */
    @Transactional
    public Map<String, Object> ejecutarRetraining(Integer optunaTrials) {
        log.info("🚀 Iniciando pipeline de auto-retraining...");
        Map<String, Object> resultado = new LinkedHashMap<>();

        try {
            // ================================
            // PASO 1: Refrescar vista materializada
            // ================================
            log.info("🔄 Refrescando vista materializada...");
            long startRefresh = System.currentTimeMillis();
            entityManager.createNativeQuery(
                    "REFRESH MATERIALIZED VIEW vw_training_dataset_morosidad")
                    .executeUpdate();
            long refreshTime = System.currentTimeMillis() - startRefresh;
            log.info("✅ Vista refrescada en {}ms", refreshTime);

            // ================================
            // PASO 2: Consultar dataset desde la vista
            // ================================
            log.info("📊 Extrayendo dataset de la vista materializada...");
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createNativeQuery(
                    "SELECT record_id, limit_bal, sex, education, marriage, age, " +
                            "pay_0, pay_2, pay_3, pay_4, pay_5, pay_6, " +
                            "bill_amt1, bill_amt2, bill_amt3, bill_amt4, bill_amt5, bill_amt6, " +
                            "pay_amt1, pay_amt2, pay_amt3, pay_amt4, pay_amt5, pay_amt6, " +
                            "utilization_rate, default_payment_next_month, sample_weight " +
                            "FROM vw_training_dataset_morosidad")
                    .getResultList();

            log.info("📊 Total de muestras extraídas: {}", rows.size());

            if (rows.isEmpty()) {
                resultado.put("error", "La vista materializada no retornó datos. ¿Hay suficiente historial?");
                return resultado;
            }

            // ================================
            // PASO 3: Mapear a DTOs
            // ================================
            List<TrainingSampleDTO> samples = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                TrainingSampleDTO sample = new TrainingSampleDTO();
                sample.setLimitBal(toDouble(row[1]));
                sample.setSex(toInt(row[2]));
                sample.setEducation(toInt(row[3]));
                sample.setMarriage(toInt(row[4]));
                sample.setAge(toInt(row[5]));
                sample.setPay0(toInt(row[6]));
                sample.setPay2(toInt(row[7]));
                sample.setPay3(toInt(row[8]));
                sample.setPay4(toInt(row[9]));
                sample.setPay5(toInt(row[10]));
                sample.setPay6(toInt(row[11]));
                sample.setBillAmt1(toDouble(row[12]));
                sample.setBillAmt2(toDouble(row[13]));
                sample.setBillAmt3(toDouble(row[14]));
                sample.setBillAmt4(toDouble(row[15]));
                sample.setBillAmt5(toDouble(row[16]));
                sample.setBillAmt6(toDouble(row[17]));
                sample.setPayAmt1(toDouble(row[18]));
                sample.setPayAmt2(toDouble(row[19]));
                sample.setPayAmt3(toDouble(row[20]));
                sample.setPayAmt4(toDouble(row[21]));
                sample.setPayAmt5(toDouble(row[22]));
                sample.setPayAmt6(toDouble(row[23]));
                sample.setUtilizationRate(toDouble(row[24]));
                sample.setDefaultPaymentNextMonth(toInt(row[25]));
                sample.setSampleWeight(toDouble(row[26]));
                samples.add(sample);
            }

            // ================================
            // PASO 4: Enviar a la API Python
            // ================================
            log.info("📡 Enviando {} muestras a la API de entrenamiento ({})...",
                    samples.size(), selfTrainingApiUrl);

            TrainingRequestDTO request = new TrainingRequestDTO();
            request.setSamples(samples);
            request.setOptunaTrials(optunaTrials != null ? optunaTrials : 30);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<TrainingRequestDTO> entity = new HttpEntity<>(request, headers);

            long startTraining = System.currentTimeMillis();
            ResponseEntity<TrainingResponseDTO> response = restTemplate.exchange(
                    selfTrainingApiUrl + "/morosidad/train",
                    HttpMethod.POST,
                    entity,
                    TrainingResponseDTO.class);
            long trainingTime = System.currentTimeMillis() - startTraining;

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                resultado.put("error", "La API de entrenamiento no retornó una respuesta válida");
                return resultado;
            }

            TrainingResponseDTO trainingResponse = response.getBody();
            log.info("✅ Entrenamiento completado en {}ms. AUC: {}",
                    trainingTime, trainingResponse.getMetrics().getAucRoc());

            // ================================
            // PASO 5: Guardar resultados en BD
            // ================================
            log.info("💾 Guardando resultados en la base de datos...");

            // Guardar DatasetInfo
            DatasetInfo datasetInfo = new DatasetInfo();
            datasetInfo.setCreationDate(LocalDateTime.now());
            datasetInfo.setDataAmount(trainingResponse.getTotalSamples());
            datasetInfo.setDataTraining(trainingResponse.getTrainSamples());
            datasetInfo.setDataTesting(trainingResponse.getTestSamples());
            datasetInfo.setSourceData("vw_training_dataset_morosidad");
            datasetInfoRepository.save(datasetInfo);

            // Guardar TrainingHistory
            TrainingHistory trainingHistory = new TrainingHistory();
            trainingHistory.setTrainingDate(LocalDateTime.now());
            trainingHistory.setMetricsResults(trainingResponse.getMetrics());
            trainingHistory.setParametersOptuna(trainingResponse.getOptunaResult());
            trainingHistory.setDatasetInfo(datasetInfo);
            trainingHistory.setInProduction(false);
            trainingHistory.setBestCadidateModel("VotingClassifier");
            trainingHistoryRepository.save(trainingHistory);

            log.info("✅ Resultados guardados. TrainingHistory ID: {}",
                    trainingHistory.getIdTrainingHistory());

            // ================================
            // Resultado final
            // ================================
            resultado.put("status", "SUCCESS");
            resultado.put("trainingId", trainingHistory.getIdTrainingHistory());
            resultado.put("datasetId", datasetInfo.getIdDataset());
            resultado.put("totalSamples", trainingResponse.getTotalSamples());
            resultado.put("trainSamples", trainingResponse.getTrainSamples());
            resultado.put("testSamples", trainingResponse.getTestSamples());
            resultado.put("classDistribution", trainingResponse.getClassDistribution());
            resultado.put("scalePosWeight", trainingResponse.getScalePosWeight());
            resultado.put("metrics", Map.of(
                    "auc_roc", trainingResponse.getMetrics().getAucRoc(),
                    "gini", trainingResponse.getMetrics().getGiniCoefficient(),
                    "ks", trainingResponse.getMetrics().getKsStatistic(),
                    "f1_score", trainingResponse.getMetrics().getF1Score(),
                    "accuracy", trainingResponse.getMetrics().getAccuracy()));
            resultado.put("refreshTimeMs", refreshTime);
            resultado.put("trainingTimeMs", trainingTime);
            resultado.put("modelSizeChars", trainingResponse.getModelBase64().length());

            return resultado;

        } catch (Exception e) {
            log.error("❌ Error en pipeline de auto-retraining: {}", e.getMessage(), e);
            resultado.put("status", "ERROR");
            resultado.put("error", e.getMessage());
            return resultado;
        }
    }

    // Utilidades de conversión segura
    private Double toDouble(Object obj) {
        if (obj == null)
            return 0.0;
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        return Double.parseDouble(obj.toString());
    }

    private Integer toInt(Object obj) {
        if (obj == null)
            return 0;
        if (obj instanceof Number)
            return ((Number) obj).intValue();
        return Integer.parseInt(obj.toString());
    }
}
