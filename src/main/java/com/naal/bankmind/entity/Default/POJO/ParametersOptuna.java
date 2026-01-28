package com.naal.bankmind.entity.Default.POJO;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ParametersOptuna implements Serializable{
    
    @JsonProperty("trial_number")
    private Integer trialNumber;

    @JsonProperty("objective_value")
    private Double objectiveValue;

    @JsonProperty("metric_optimized")
    private String metricOptimized;

    @JsonProperty("best_params")
    private Map<String, Object> bestParams;

    /*
        {
        "trial_number": 45,
        "objective_value": 0.7915,
        "metric_optimized": "auc",
        "search_space_hash": "config_v3",
        "best_params": {
            "xgb_learning_rate": 0.035,
            "xgb_max_depth": 7,
            "lgbm_num_leaves": 40,
            "rf_n_estimators": 150,
            "ensemble_weight_xgb": 3,
            "ensemble_weight_lgbm": 2,
            "ensemble_weight_rf": 1
        }
        }
    */
}
