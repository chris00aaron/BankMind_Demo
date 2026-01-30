package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO Request para enviar lote de transacciones a la API Python
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchApiRequestDto {

    @JsonProperty("transactions")
    private List<FraudPredictionRequestDto> transactions;
}
