package com.naal.bankmind.dto.Default.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.naal.bankmind.entity.Default.POJO.DetalleColumna;
import com.naal.bankmind.entity.Default.POJO.MetricsResults;
import com.naal.bankmind.entity.Default.POJO.ParametersOptuna;
import com.naal.bankmind.entity.Default.POJO.AssemblyConfiguration;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO para recibir la respuesta de entrenamiento de la API Python.
 * Solo contiene campos que se almacenan en BD o controlan el flujo.
 */
@Data
@NoArgsConstructor
public class TrainingResponseDTO {

    private MetricsResults metrics;

    @JsonProperty("optuna_result")
    private ParametersOptuna optunaResult;

    @JsonProperty("total_samples")
    private Integer totalSamples;

    @JsonProperty("train_samples")
    private Integer trainSamples;

    @JsonProperty("test_samples")
    private Integer testSamples;

    @JsonProperty("baseline_distributions")
    private Map<String, Object> baselineDistributions;

    @JsonProperty("deployment_status")
    private String deploymentStatus;

    @JsonProperty("assembly_config")
    private AssemblyConfiguration assemblyConfig;

    @JsonProperty("dagshub_verified")
    private Boolean dagshubVerified;

    @JsonProperty("version_tag")
    private String versionTag;

    @JsonProperty("columns_info")
    private List<DetalleColumna> columnsInfo;

    @JsonProperty("dataset_start_date")
    private String datasetStartDate;
}
