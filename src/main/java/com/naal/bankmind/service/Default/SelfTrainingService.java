package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Request.TrainingRequestDTO;
import com.naal.bankmind.dto.Default.Response.TrainingResponseDTO;
import com.naal.bankmind.entity.Default.DatasetInfo;
import com.naal.bankmind.entity.Default.TrainingHistory;
import com.naal.bankmind.entity.Default.ProductionModelDefault;
import com.naal.bankmind.client.Default.MorosidadFeignClient;
import com.naal.bankmind.client.Default.SelfTrainingFeignClient;
import com.naal.bankmind.repository.Default.DatasetInfoRepository;
import com.naal.bankmind.repository.Default.TrainingHistoryRepository;
import com.naal.bankmind.repository.Default.ProductionModelDefaultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio de orquestación (versión ligera) para el auto-retraining.
 * 
 * Nueva Responsabilidad (V7):
 * 1. Disparar entrenamiento en API Python (Trigger).
 * 2. Recibir resultados y métricas.
 * 3. Guardar historial en BD.
 * 4. Si Python indica "NEW_CHAMPION", solicitar Hot Reload a API Predicción.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfTrainingService {

    private final TrainingHistoryRepository trainingHistoryRepository;
    private final DatasetInfoRepository datasetInfoRepository;
    private final ProductionModelDefaultRepository productionModelRepository;
    private final SelfTrainingFeignClient selfTrainingFeignClient;
    private final MorosidadFeignClient morosidadFeignClient;

    /**
     * Ejecuta el trigger de auto-retraining.
     */
    @Transactional
    public Map<String, Object> ejecutarRetraining(Integer optunaTrials) {
        log.info("🚀 Iniciando trigger de auto-retraining (Orquestación Python)...");
        Map<String, Object> resultado = new LinkedHashMap<>();

        try {
            // ================================
            // PASO 1: Disparar entrenamiento (Trigger)
            // ================================
            TrainingRequestDTO request = new TrainingRequestDTO();
            request.setOptunaTrials(optunaTrials != null ? optunaTrials : 30);

            log.info("📡 Llamando a API Python (Trials={})...", request.getOptunaTrials());
            long start = System.currentTimeMillis();

            TrainingResponseDTO response = selfTrainingFeignClient.triggerTraining(request);

            long duration = System.currentTimeMillis() - start;

            if (response == null) {
                resultado.put("error", "Respuesta nula de API Python");
                return resultado;
            }
            log.info("✅ Respuesta recibida en {}ms. Status: {}", duration, response.getDeploymentStatus());

            // ================================
            // PASO 2: Guardar Metadata del Dataset
            // ================================
            DatasetInfo datasetInfo = new DatasetInfo();
            datasetInfo.setCreationDate(LocalDateTime.now());
            datasetInfo.setDataAmount(response.getTotalSamples());
            datasetInfo.setDataTraining(response.getTrainSamples());
            datasetInfo.setDataTesting(response.getTestSamples());
            datasetInfo.setSourceData("vw_training_dataset_morosidad (Python DB Connect)");

            // Columnas usadas en el entrenamiento
            if (response.getColumnsInfo() != null) {
                datasetInfo.setColumnsInfo(response.getColumnsInfo());
            }

            // Fecha inicio del dataset
            if (response.getDatasetStartDate() != null) {
                try {
                    datasetInfo.setStartDate(
                            LocalDateTime.parse(response.getDatasetStartDate().replace(" ", "T").substring(0, 19)));
                } catch (Exception ex) {
                    log.warn("⚠️ No se pudo parsear dataset_start_date: {}", response.getDatasetStartDate());
                }
            }

            datasetInfoRepository.save(datasetInfo);

            // ================================
            // PASO 3: Guardar Historial
            // ================================
            TrainingHistory history = new TrainingHistory();
            history.setTrainingDate(LocalDateTime.now());
            history.setMetricsResults(response.getMetrics());
            history.setParametersOptuna(response.getOptunaResult());
            history.setBaselineDistributions(response.getBaselineDistributions());
            history.setDatasetInfo(datasetInfo);
            history.setBestCadidateModel("Ensemble (Voting)");

            // Enlazar con el modelo actualmente en producción
            Optional<TrainingHistory> currentProd = trainingHistoryRepository.findByInProductionTrue();
            if (currentProd.isPresent()) {
                history.setIdTrainingModel(currentProd.get().getIdTrainingHistory());
            }

            // ================================
            // PASO 4: Manejo de Status (Promoción)
            // ================================
            String status = response.getDeploymentStatus();
            boolean isNewChampion = "NEW_CHAMPION".equals(status);

            if (isNewChampion) {
                // ══════════════════════════════════════
                // VERIFICACIÓN TRANSACCIONAL
                // ══════════════════════════════════════
                if (!Boolean.TRUE.equals(response.getDagshubVerified())) {
                    log.error("❌ ABORT: DagsHub no verificó el upload. No se promociona el modelo.");
                    history.setInProduction(false);
                    trainingHistoryRepository.save(history);

                    resultado.put("status", "DAGSHUB_VERIFICATION_FAILED");
                    resultado.put("error", "El modelo no fue verificado en DagsHub. Se cancela la promoción.");
                    resultado.put("deploymentStatus", "UPLOAD_FAILED");
                    resultado.put("metrics", response.getMetrics());
                    resultado.put("durationMs", duration);
                    return resultado;
                }

                log.info("🏆 ¡NUEVO CHAMPION VERIFICADO! Actualizando sistema...");

                // 1. Desactivar históricos en training_history
                trainingHistoryRepository.deactivateAllModels();
                history.setInProduction(true);

                // 2. Retirar modelo de producción anterior y crear nuevo
                productionModelRepository.retireAllActiveModels();

                ProductionModelDefault prodModel = new ProductionModelDefault();
                String version = response.getVersionTag() != null ? response.getVersionTag()
                        : "v_" + System.currentTimeMillis();
                prodModel.setVersion(version);
                prodModel.setDeploymentDate(LocalDateTime.now());
                prodModel.setAucRoc(String.valueOf(response.getMetrics().getAucRoc()));
                prodModel.setGiniCoefficient(String.valueOf(response.getMetrics().getGiniCoefficient()));
                prodModel.setKsStatistic(String.valueOf(response.getMetrics().getKsStatistic()));
                prodModel.setAssemblyConfiguration(response.getAssemblyConfig());
                prodModel.setIsActive(true);
                productionModelRepository.save(prodModel);
                log.info("✅ Modelo de producción registrado: {}", prodModel.getVersion());

                // 3. Trigger Hot Reload (API Predicción)
                triggerHotReload();

            } else {
                log.info("📉 Se mantiene el Champion actual.");
                history.setInProduction(false);
            }

            trainingHistoryRepository.save(history);

            // Output Final
            resultado.put("status", "SUCCESS");
            resultado.put("trainingId", history.getIdTrainingHistory());
            resultado.put("deploymentStatus", status);
            resultado.put("metrics", response.getMetrics());
            resultado.put("durationMs", duration);

            return resultado;

        } catch (Exception e) {
            log.error("❌ Error en trigger de auto-retraining: {}", e.getMessage(), e);
            resultado.put("status", "ERROR");
            resultado.put("error", e.getMessage());
            return resultado;
        }
    }

    private void triggerHotReload() {
        try {
            log.info("♻️ Solicitando Hot Reload a API de Predicción...");
            morosidadFeignClient.refreshModel();
            log.info("✅ Solicitud enviada correctamente.");
        } catch (Exception e) {
            log.error("❌ Error solicitando Hot Reload: {}", e.getMessage());
        }
    }
}
