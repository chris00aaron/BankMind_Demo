package com.naal.bankmind.dto.Default.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar entrenamiento a la API Python.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainingRequestDTO {

    @JsonProperty("optuna_trials")
    private Integer optunaTrials = 30;
}
