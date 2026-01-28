package com.naal.bankmind.entity.Default.POJO;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MetricsResults implements Serializable {

    @JsonProperty("auc_roc")
    private Double aucRoc;

    @JsonProperty("ks_statistic")
    private Double ksStatistic;

    @JsonProperty("gini_coefficient")
    private Double giniCoefficient;

    private Double accuracy;
    private Double precision;
    private Double recall;

    @JsonProperty("f1_score")
    private Double f1Score;

    @JsonProperty("training_time_sec")
    private Double trainingTimeSec;

    /*
     * EJEMPLO DE JSON
     * {
     * "auc_roc": 0.7807,
     * "ks_statistic": 0.4512,
     * "gini_coefficient": 0.5614,
     * "accuracy": 0.7303,
     * "precision": 0.4289,
     * "recall": 0.6623,
     * "f1_score": 0.5206,
     * "training_time_sec": 345.5,
     * }
     */

}
