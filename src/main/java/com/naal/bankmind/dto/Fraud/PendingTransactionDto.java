package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para representar una transacción pendiente de análisis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTransactionDto {

    @JsonProperty("id_transaction")
    private Long idTransaction;

    @JsonProperty("trans_num")
    private String transNum;

    @JsonProperty("trans_date_time")
    private LocalDateTime transDateTime;

    @JsonProperty("amt")
    private Double amt;

    @JsonProperty("category")
    private String category;

    @JsonProperty("merchant")
    private String merchant;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("cc_num_masked")
    private String ccNumMasked; // Últimos 4 dígitos
}
