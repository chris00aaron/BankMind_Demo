package com.naal.bankmind.dto.Default.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.naal.bankmind.entity.Default.POJO.AssemblyConfiguration;
import com.naal.bankmind.entity.Default.POJO.MetricsResults;
import com.naal.bankmind.entity.Default.POJO.ParametersOptuna;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para recibir la respuesta de entrenamiento de la API Python.
 */
@Data
@NoArgsConstructor
public class TrainingResponseDTO {

    private MetricsResults metrics;

    @JsonProperty("optuna_result")
    private ParametersOptuna optunaResult;

    @JsonProperty("model_base64")
    private String modelBase64;

    @JsonProperty("assembly_config")
    private AssemblyConfiguration assemblyConfig;

    @JsonProperty("total_samples")
    private Integer totalSamples;

    @JsonProperty("train_samples")
    private Integer trainSamples;

    @JsonProperty("test_samples")
    private Integer testSamples;

    @JsonProperty("class_distribution")
    private Map<String, Integer> classDistribution;

    @JsonProperty("scale_pos_weight")
    private Double scalePosWeight;
}
