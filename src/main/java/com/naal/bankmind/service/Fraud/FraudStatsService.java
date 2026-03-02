package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.CategoryStatsDto;
import com.naal.bankmind.dto.Fraud.DashboardStatsDto;
import com.naal.bankmind.dto.Fraud.DemographicStatsDto;
import com.naal.bankmind.dto.Fraud.HourlyTrendDto;
import com.naal.bankmind.dto.Fraud.LocationStatsDto;
import com.naal.bankmind.dto.Fraud.ShapGlobalDto;
import com.naal.bankmind.dto.Fraud.TemporalStatsDto;
import com.naal.bankmind.repository.Fraud.CreditCardRepository;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import com.naal.bankmind.repository.Fraud.PredictionDetailsRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio especializado en ESTADÍSTICAS de fraude para el Dashboard.
 *
 * Responsabilidad única: agregaciones, tendencias y métricas globales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudStatsService {

    private final FraudPredictionRepository fraudPredictionRepository;
    private final PredictionDetailsRepository predictionDetailsRepository;
    private final TransactionRepository transactionRepository;
    private final CreditCardRepository creditCardRepository;

    /**
     * Obtiene estadísticas consolidadas para el dashboard.
     */
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long total = fraudPredictionRepository.count();
        long frauds = fraudPredictionRepository.countByVeredicto(FraudConstants.VEREDICTO_ALTO_RIESGO);
        long legitimate = fraudPredictionRepository.countByVeredicto(FraudConstants.VEREDICTO_LEGITIMO);
        Double amountAtRisk = fraudPredictionRepository.sumAmountAtRisk();
        Double avgScore = fraudPredictionRepository.avgFraudScore();

        double fraudRate = total > 0 ? (frauds * 100.0 / total) : 0.0;

        long pendingCount = transactionRepository.countByStatus(FraudConstants.STATUS_PENDING);
        long approvedCount = transactionRepository.countByStatus(FraudConstants.STATUS_APPROVED);
        long rejectedCount = transactionRepository.countByStatus(FraudConstants.STATUS_REJECTED);
        long cardsBlocked = creditCardRepository.countByIsActive(false);

        return DashboardStatsDto.builder()
                .transactionsToday(total)
                .fraudsDetected(frauds)
                .legitimate(legitimate)
                .fraudRate(Math.round(fraudRate * 100.0) / 100.0)
                .totalAmountAtRisk(amountAtRisk != null ? amountAtRisk : 0.0)
                .avgFraudScore(avgScore != null ? Math.round(avgScore * 10000.0) / 10000.0 : 0.0)
                .pendingCount(pendingCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .cardsBlockedToday(cardsBlocked)
                .build();
    }

    /**
     * Obtiene tendencia horaria de transacciones y fraudes.
     */
    @Transactional(readOnly = true)
    public List<HourlyTrendDto> getHourlyTrend() {
        return fraudPredictionRepository.getHourlyTrendStats().stream()
                .map(row -> {
                    int hour = ((Number) row[0]).intValue();
                    long total = ((Number) row[1]).longValue();
                    long frauds = ((Number) row[2]).longValue();
                    double rate = total > 0 ? (frauds * 100.0 / total) : 0.0;
                    return HourlyTrendDto.builder()
                            .hour(hour)
                            .totalTransactions(total)
                            .fraudCount(frauds)
                            .fraudRate(Math.round(rate * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas SHAP globales (factores más influyentes).
     */
    @Transactional(readOnly = true)
    public List<ShapGlobalDto> getGlobalShapStats() {
        return predictionDetailsRepository.getGlobalShapStats().stream()
                .map(row -> {
                    String featureName = (String) row[0];
                    double avgImpact = ((Number) row[1]).doubleValue();
                    long occurrences = ((Number) row[2]).longValue();
                    return ShapGlobalDto.builder()
                            .featureName(featureName)
                            .avgImpact(Math.round(avgImpact * 100.0) / 100.0)
                            .occurrences(occurrences)
                            .displayName(translateFeatureName(featureName))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas por categoría de comercio.
     */
    @Transactional(readOnly = true)
    public List<CategoryStatsDto> getCategoryStats() {
        return fraudPredictionRepository.getCategoryStats().stream()
                .map(row -> {
                    String category = (String) row[0];
                    long total = ((Number) row[1]).longValue();
                    long frauds = ((Number) row[2]).longValue();
                    double amt = ((Number) row[3]).doubleValue();
                    double rate = total > 0 ? (frauds * 100.0 / total) : 0.0;
                    return CategoryStatsDto.builder()
                            .category(category != null ? category : "Sin categoría")
                            .totalTransactions(total)
                            .fraudCount(frauds)
                            .fraudRate(Math.round(rate * 100.0) / 100.0)
                            .totalAmount(amt)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas por ubicación geográfica.
     */
    @Transactional(readOnly = true)
    public List<LocationStatsDto> getLocationStats() {
        return fraudPredictionRepository.getLocationStats().stream()
                .map(row -> {
                    String state = (String) row[0];
                    long total = ((Number) row[1]).longValue();
                    long frauds = ((Number) row[2]).longValue();
                    double rate = total > 0 ? (frauds * 100.0 / total) : 0.0;
                    return LocationStatsDto.builder()
                            .state(state != null ? state : "Desconocido")
                            .city(null)
                            .fraudCount(frauds)
                            .totalTransactions(total)
                            .fraudRate(Math.round(rate * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Traduce nombres de features técnicos a nombres legibles para el dashboard.
     */
    private String translateFeatureName(String featureName) {
        if (featureName == null)
            return "Desconocido";
        return switch (featureName.toLowerCase()) {
            case "amt" -> "Monto Elevado";
            case "hour" -> "Hora Inusual";
            case "distance_km" -> "Distancia Lejana";
            case "category" -> "Categoría Riesgo";
            case "age" -> "Edad del Cliente";
            case "anomaly_score" -> "Score Anomalía";
            case "city_pop" -> "Población Ciudad";
            case "merch_lat", "merch_long" -> "Ubicación Comercio";
            default -> featureName.replace("_", " ");
        };
    }

    // ─── Analytics: Demografía y Temporal ────────────────────────────────────

    /**
     * Distribución de fraudes por género y rango de edad.
     * Alimenta el panel "Perfil del Defraudador" del Dashboard.
     */
    @Transactional(readOnly = true)
    public List<DemographicStatsDto> getDemographicStats() {
        return fraudPredictionRepository.getDemographicStats().stream()
                .map(row -> DemographicStatsDto.builder()
                        .genderLabel((String) row[0])
                        .ageBand((String) row[1])
                        .fraudCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Distribución de fraudes por día de la semana y mes.
     * Alimenta el panel "¿Cuándo ocurre el fraude?" del Dashboard.
     */
    @Transactional(readOnly = true)
    public List<TemporalStatsDto> getTemporalStats() {
        return fraudPredictionRepository.getTemporalStats().stream()
                .map(row -> TemporalStatsDto.builder()
                        .dayOfWeek(((Number) row[0]).intValue())
                        .monthLabel((String) row[1])
                        .fraudCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());
    }
}
