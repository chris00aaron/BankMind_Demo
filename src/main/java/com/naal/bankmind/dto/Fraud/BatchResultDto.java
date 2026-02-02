package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta de procesamiento por lotes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResultDto {

    @JsonProperty("total_processed")
    private int totalProcessed;

    @JsonProperty("total_frauds")
    private int totalFrauds;

    @JsonProperty("total_legitimate")
    private int totalLegitimate;

    @JsonProperty("total_errors")
    private int totalErrors;

    @JsonProperty("results")
    private List<BatchItemResultDto> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchItemResultDto {
        @JsonProperty("id_transaction")
        private Long idTransaction;

        @JsonProperty("trans_num")
        private String transNum;

        @JsonProperty("amt")
        private Double amt;

        @JsonProperty("veredicto")
        private String veredicto;

        @JsonProperty("score")
        private Float score;

        @JsonProperty("status")
        private String status; // "success" | "error"

        @JsonProperty("error_message")
        private String errorMessage;
    }
}
