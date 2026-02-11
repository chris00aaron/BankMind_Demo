package com.naal.bankmind.dto.Default.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para solicitar entrenamiento a la API Python.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainingRequestDTO {

    private List<TrainingSampleDTO> samples;

    @JsonProperty("optuna_trials")
    private Integer optunaTrials = 30;
}
