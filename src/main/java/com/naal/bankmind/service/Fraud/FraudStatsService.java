package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import com.naal.bankmind.repository.Fraud.PredictionDetailsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio especializado en ESTADÍSTICAS de fraude para Dashboard
 * Responsabilidad: Agregaciones, tendencias, métricas globales
 * 
 * Principio SOLID: Single Responsibility
 */
@Service
public class FraudStatsService {

    private final FraudPredictionRepository fraudPredictionRepository;
    private final PredictionDetailsRepository predictionDetailsRepository;
    private final com.naal.bankmind.repository.Fraud.TransactionRepository transactionRepository;
    private final com.naal.bankmind.repository.Fraud.CreditCardRepository creditCardRepository;

    public FraudStatsService(
            FraudPredictionRepository fraudPredictionRepository,
            PredictionDetailsRepository predictionDetailsRepository,
            com.naal.bankmind.repository.Fraud.TransactionRepository transactionRepository,
            com.naal.bankmind.repository.Fraud.CreditCardRepository creditCardRepository) {
        this.fraudPredictionRepository = fraudPredictionRepository;
        this.predictionDetailsRepository = predictionDetailsRepository;
        this.transactionRepository = transactionRepository;
        this.creditCardRepository = creditCardRepository;
    }

    /**
     * Obtiene estadísticas consolidadas para el dashboard
     */
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        // Datos existentes
        long total = fraudPredictionRepository.count();
        long frauds = fraudPredictionRepository.countByVeredicto("ALTO RIESGO");
        long legitimate = fraudPredictionRepository.countByVeredicto("LEGÍTIMO");
        Double amountAtRisk = fraudPredictionRepository.sumAmountAtRisk();
        Double avgScore = fraudPredictionRepository.avgFraudScore();

        double fraudRate = total > 0 ? (frauds * 100.0 / total) : 0.0;

        // ========== NUEVOS DATOS: Sistema de Notificaciones ==========

        // 1. Estados de transacciones (DATOS REALES)
        long pendingCount = transactionRepository.countByStatus("PENDING");
        long approvedCount = transactionRepository.countByStatus("APPROVED");
        long rejectedCount = transactionRepository.countByStatus("REJECTED");

        // 2. Tarjetas bloqueadas (isActive = false - DATOS REALES)
        long cardsBlocked = creditCardRepository.countByIsActive(false);

        return DashboardStatsDto.builder()
                .transactionsToday(total)
                .fraudsDetected(frauds)
                .legitimate(legitimate)
                .fraudRate(Math.round(fraudRate * 100.0) / 100.0)
                .totalAmountAtRisk(amountAtRisk != null ? amountAtRisk : 0.0)
                .avgFraudScore(avgScore != null ? Math.round(avgScore * 10000.0) / 10000.0 : 0.0)
                // Nuevos campos
                .pendingCount(pendingCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .cardsBlockedToday(cardsBlocked)
                .build();
    }

    /**
     * Obtiene tendencia horaria de transacciones y fraudes
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
     * Obtiene estadísticas SHAP globales (factores más influyentes)
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
     * Obtiene estadísticas por categoría de comercio
     */
    @Transactional(readOnly = true)
    public List<CategoryStatsDto> getCategoryStats() {
        return fraudPredictionRepository.getCategoryStats().stream()
                .map(row -> {
                    String category = (String) row[0];
                    long total = ((Number) row[1]).longValue();
                    long frauds = ((Number) row[2]).longValue();
                    double amount = ((Number) row[3]).doubleValue();
                    double rate = total > 0 ? (frauds * 100.0 / total) : 0.0;

                    return CategoryStatsDto.builder()
                            .category(category != null ? category : "Sin categoría")
                            .totalTransactions(total)
                            .fraudCount(frauds)
                            .fraudRate(Math.round(rate * 100.0) / 100.0)
                            .totalAmount(amount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas por ubicación geográfica
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
     * Traduce nombres de features técnicos a nombres legibles
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
}
