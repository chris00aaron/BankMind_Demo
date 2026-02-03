package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Response.ModelHealthDTO;
import com.naal.bankmind.dto.Default.Response.ModelHealthDTO.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Servicio para obtener datos del modelo en producción.
 * Retorna mockdata cuando no hay modelo en BD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelHealthService {

    private final EntityManager entityManager;

    /**
     * Obtiene datos del modelo en producción.
     * Si no hay modelo, retorna mockdata.
     */
    public ModelHealthDTO getModelHealth() {
        log.info("Obteniendo estado del modelo en producción");

        // Intentar obtener modelo real
        ModelHealthDTO realData = fetchRealModelData();
        if (realData != null) {
            return realData;
        }

        // Si no hay datos, retornar mockdata
        log.info("No hay modelo en BD, retornando mockdata");
        return generateMockData();
    }

    /**
     * Intenta obtener datos reales del modelo.
     */
    @SuppressWarnings("unchecked")
    private ModelHealthDTO fetchRealModelData() {
        try {
            String query = "SELECT th FROM TrainingHistory th WHERE th.inProduction = true ORDER BY th.trainingDate DESC";
            List<?> results = entityManager.createQuery(query).setMaxResults(1).getResultList();

            if (results.isEmpty()) {
                return null;
            }
            // TODO: Mapear datos reales cuando existan
            return null;
        } catch (Exception e) {
            log.warn("Error al obtener modelo real: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Genera datos mock realistas para la vista.
     */
    private ModelHealthDTO generateMockData() {
        ModelHealthDTO dto = new ModelHealthDTO();

        // Estado del modelo
        LocalDateTime deploymentDate = LocalDateTime.now().minusDays(46);
        dto.setVersion("Default Predictor v2.1");
        dto.setDeploymentDate(deploymentDate);
        dto.setDaysActive(ChronoUnit.DAYS.between(deploymentDate, LocalDateTime.now()));
        dto.setActive(true);

        // Métricas
        dto.setMetricas(new MetricasModelo(
                0.7807, // AUC-ROC
                0.7303, // Precision
                0.6623, // Recall
                0.5206, // F1-Score
                0.5614, // Gini
                0.4512, // KS
                0.7303 // Accuracy
        ));

        // Arquitectura del ensamble
        List<ComponenteModelo> componentes = new ArrayList<>();

        Map<String, Object> xgbParams = new HashMap<>();
        xgbParams.put("n_estimators", 650);
        xgbParams.put("learning_rate", 0.045);
        xgbParams.put("max_depth", 6);
        componentes.add(new ComponenteModelo("XGBoost", 2, xgbParams));

        Map<String, Object> lgbmParams = new HashMap<>();
        lgbmParams.put("n_estimators", 500);
        lgbmParams.put("num_leaves", 31);
        lgbmParams.put("learning_rate", 0.05);
        componentes.add(new ComponenteModelo("LightGBM", 1, lgbmParams));

        Map<String, Object> rfParams = new HashMap<>();
        rfParams.put("n_estimators", 100);
        rfParams.put("max_depth", 10);
        rfParams.put("criterion", "gini");
        componentes.add(new ComponenteModelo("Random Forest", 1, rfParams));

        dto.setArquitectura(new ArquitecturaModelo("VotingClassifier", "soft", componentes));

        // Tendencia de últimos 12 meses
        dto.setTendencia(generateTrendData());

        // Dataset
        dto.setDataset(new DatasetResumen(
                30000,
                24000,
                6000,
                LocalDateTime.now().minusMonths(2),
                "monthly_history"));

        return dto;
    }

    /**
     * Genera datos de tendencia realistas.
     */
    private List<TendenciaRendimiento> generateTrendData() {
        List<TendenciaRendimiento> trend = new ArrayList<>();
        String[] meses = { "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic", "Ene" };

        // Datos realistas de morosidad
        double[] morosidadReal = { 18.5, 17.2, 19.8, 21.3, 20.1, 22.5, 21.8, 23.2, 22.1, 24.5, 23.8, 25.2 };
        double[] prediccion = { 17.8, 18.1, 19.2, 20.5, 21.2, 21.8, 22.5, 22.8, 23.1, 23.9, 24.5, 24.8 };

        for (int i = 0; i < meses.length; i++) {
            trend.add(new TendenciaRendimiento(
                    meses[i],
                    morosidadReal[i],
                    prediccion[i],
                    Math.round((prediccion[i] - morosidadReal[i]) * 10.0) / 10.0));
        }

        return trend;
    }
}
