package com.naal.bankmind.dto.Fraud;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de un perfil de cluster de fraude.
 * Serializado por FraudController hacia el Frontend.
 */
@Data
@Builder
public class ClusterProfileDto {

    private int clusterId;
    private String label;
    private int fraudCount;
    private double pctOfTotal;

    // Centroids numéricos
    private double avgAmount;
    private double avgHour;
    private double avgAge;
    private double avgDistanceKm;

    // Categóricos dominantes
    private String topCategory;
    private String topState;
}
