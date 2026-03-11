package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * DTO de la respuesta completa del endpoint de clustering en Python.
 * Usada por FraudApiClient para deserializar el JSON de vuelta de Python.
 *
 * Campos en snake_case para coincidir con la respuesta Python/FastAPI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusteringApiResponseDto {

    private List<ClusterProfileApiDto> profiles;

    @JsonProperty("total_frauds_analyzed")
    private int totalFraudsAnalyzed;

    @JsonProperty("n_clusters_used")
    private int nClustersUsed;

    public int getNclustersUsed() {
        return nClustersUsed;
    }

    @JsonProperty("run_date")
    private String runDate;

    private String message;

    /**
     * DTO interno para cada perfil recibido de Python.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterProfileApiDto {

        @JsonProperty("cluster_id")
        private int clusterId;

        private String label;

        @JsonProperty("fraud_count")
        private int fraudCount;

        @JsonProperty("pct_of_total")
        private double pctOfTotal;

        @JsonProperty("avg_amount")
        private double avgAmount;

        @JsonProperty("avg_hour")
        private double avgHour;

        @JsonProperty("avg_age")
        private double avgAge;

        @JsonProperty("avg_distance_km")
        private double avgDistanceKm;

        @JsonProperty("avg_city_pop")
        private double avgCityPop;

        @JsonProperty("top_category")
        private String topCategory;

        @JsonProperty("top_state")
        private String topState;
    }
}
