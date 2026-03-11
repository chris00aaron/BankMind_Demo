package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.ClusterProfileDto;
import com.naal.bankmind.dto.Fraud.ClusteringApiResponseDto;
import com.naal.bankmind.entity.Fraud.FraudClusterProfile;
import com.naal.bankmind.repository.Fraud.ClusterProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de Clustering de Fraude.
 *
 * Responsabilidad única (SRP): orquesta el pipeline completo de clustering:
 * 1. Llama a la API Python de self-training (puerto 8001) via RestTemplate.
 * 2. Persiste los nuevos perfiles en fraud_cluster_profiles.
 * 3. Limpia runs históricos según política de retención.
 * 4. Expone los perfiles del último run para el Dashboard.
 *
 * Nota de arquitectura: se usa RestTemplate (schedulerRestTemplate bean)
 * en lugar de FraudApiClient/WebClient porque la API de clustering está en
 * self-training.api.base-url (puerto 8001), no en api.base-url (puerto 8000).
 * Este mismo patrón lo sigue FraudTrainingScheduler.
 */
@Slf4j
@Service
public class FraudClusteringService {

    private static final int DEFAULT_N_CLUSTERS = 3;
    private static final int DEFAULT_MIN_SAMPLES = 30;

    /**
     * Retención: conservamos los últimos 4 runs (aprox. 1 mes con scheduler
     * semanal).
     */
    private static final int MAX_RUNS_TO_KEEP = 4;

    private final ClusterProfileRepository clusterProfileRepository;
    private final RestTemplate schedulerRestTemplate;

    @Value("${fraud.scheduler.clustering-url}")
    private String clusteringApiUrl;

    public FraudClusteringService(
            ClusterProfileRepository clusterProfileRepository,
            RestTemplate schedulerRestTemplate) {
        this.clusterProfileRepository = clusterProfileRepository;
        this.schedulerRestTemplate = schedulerRestTemplate;
    }

    // ─── Consulta ────────────────────────────────────────────────────────────

    /**
     * Devuelve los perfiles del run más reciente para el Dashboard.
     * Si nunca se ha ejecutado el análisis, devuelve lista vacía.
     */
    @Transactional(readOnly = true)
    public List<ClusterProfileDto> getLatestProfiles() {
        LocalDateTime latestRun = clusterProfileRepository.findLatestRunDate();
        if (latestRun == null) {
            log.info("[CLUSTERING] No hay perfiles en BD todavía.");
            return List.of();
        }
        return clusterProfileRepository
                .findByRunDateOrderByFraudCountDesc(latestRun)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ─── Cómputo y persistencia ──────────────────────────────────────────────

    /**
     * Orquesta la ejecución completa de clustering con parámetros por defecto.
     */
    @Transactional
    public List<ClusterProfileDto> computeAndPersist() {
        return computeAndPersist(DEFAULT_N_CLUSTERS, DEFAULT_MIN_SAMPLES);
    }

    /**
     * Versión parametrizable para el scheduler o llamadas manuales.
     *
     * Llama a la API Python de self-training (puerto 8001) usando RestTemplate,
     * el mismo patrón que FraudTrainingScheduler.
     */
    @Transactional
    public List<ClusterProfileDto> computeAndPersist(int nClusters, int minSamples) {
        log.info("[CLUSTERING] Iniciando cómputo (K={}, minSamples={}).", nClusters, minSamples);

        // ── 1. Llamar Python self-training API (puerto 8001) ──────
        ClusteringApiResponseDto apiResponse = callClusteringApi(nClusters, minSamples);

        if (apiResponse == null || apiResponse.getProfiles() == null || apiResponse.getProfiles().isEmpty()) {
            log.warn("[CLUSTERING] La API Python devolvió respuesta vacía. Posiblemente muestras insuficientes.");
            return List.of();
        }

        // ── 2. Timestamp del run ──────────────────────────────────
        LocalDateTime runDate = LocalDateTime.now();
        int clustersUsed = apiResponse.getNclustersUsed();

        // ── 3. Persistir perfiles ─────────────────────────────────
        List<FraudClusterProfile> entities = apiResponse.getProfiles().stream()
                .map(p -> toEntity(p, runDate, clustersUsed))
                .collect(Collectors.toList());

        clusterProfileRepository.saveAll(entities);
        log.info("[CLUSTERING] {} perfiles persistidos (run: {}).", entities.size(), runDate);

        // ── 4. Política de retención ──────────────────────────────
        applyRetentionPolicy();

        // ── 5. Devolver DTOs del run recién creado ────────────────
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // ─── HTTP call a Python ───────────────────────────────────────────────────

    /**
     * Llama POST {clusteringApiUrl} con RestTemplate síncrono.
     * Usa el mismo bean (schedulerRestTemplate) que el FraudTrainingScheduler
     * para garantizar el timeout correcto hacia la API de self-training.
     */
    private ClusteringApiResponseDto callClusteringApi(int nClusters, int minSamples) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "n_clusters", nClusters,
                "min_samples", minSamples);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("[CLUSTERING] Llamando API Python: POST {}", clusteringApiUrl);
        ResponseEntity<ClusteringApiResponseDto> response = schedulerRestTemplate.exchange(
                clusteringApiUrl,
                HttpMethod.POST,
                request,
                ClusteringApiResponseDto.class);

        return response.getBody();
    }

    // ─── Utilidades privadas ──────────────────────────────────────────────────

    /**
     * Convierte un ClusterProfileApiDto en entidad JPA.
     */
    private FraudClusterProfile toEntity(
            ClusteringApiResponseDto.ClusterProfileApiDto p,
            LocalDateTime runDate,
            int nClusters) {
        return FraudClusterProfile.builder()
                .runDate(runDate)
                .nClusters(nClusters)
                .clusterId(p.getClusterId())
                .fraudCount(p.getFraudCount())
                .pctOfTotal(p.getPctOfTotal())
                .avgAmount(p.getAvgAmount())
                .avgHour(p.getAvgHour())
                .avgAge(p.getAvgAge())
                .avgDistanceKm(p.getAvgDistanceKm())
                .topCategory(p.getTopCategory())
                .topState(p.getTopState())
                .label(p.getLabel())
                .build();
    }

    /**
     * Elimina runs cuya antigüedad supera MAX_RUNS_TO_KEEP semanas.
     */
    private void applyRetentionPolicy() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays((long) MAX_RUNS_TO_KEEP * 7);
        clusterProfileRepository.deleteByRunDateBefore(cutoff);
        log.debug("[CLUSTERING] Retención aplicada: eliminados runs anteriores a {}.", cutoff);
    }

    private ClusterProfileDto mapToDto(FraudClusterProfile entity) {
        return ClusterProfileDto.builder()
                .clusterId(entity.getClusterId())
                .label(entity.getLabel())
                .fraudCount(entity.getFraudCount())
                .pctOfTotal(entity.getPctOfTotal() != null ? entity.getPctOfTotal() : 0.0)
                .avgAmount(entity.getAvgAmount() != null ? entity.getAvgAmount() : 0.0)
                .avgHour(entity.getAvgHour() != null ? entity.getAvgHour() : 0.0)
                .avgAge(entity.getAvgAge() != null ? entity.getAvgAge() : 0.0)
                .avgDistanceKm(entity.getAvgDistanceKm() != null ? entity.getAvgDistanceKm() : 0.0)
                .topCategory(entity.getTopCategory())
                .topState(entity.getTopState())
                .build();
    }
}
