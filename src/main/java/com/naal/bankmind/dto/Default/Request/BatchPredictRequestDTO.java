package com.naal.bankmind.dto.Default.Request;

import lombok.Data;
import java.util.List;

/**
 * DTO para solicitar predicción batch con lista de recordIds.
 */
@Data
public class BatchPredictRequestDTO {

    private List<Long> recordIds;
}
