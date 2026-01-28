package com.naal.bankmind.entity.Default.POJO;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AssemblyConfiguration {
    private String architecture;

    @JsonProperty("voting_strategy")
    private String votingStrategy;

    @JsonProperty("weights_assigned")
    private List<Integer> weightsAssigned;

    @JsonProperty("order_estimators")
    private List<String> orderEstimators;

    @JsonProperty("random_seed")
    private Integer randomSeed;

    @JsonProperty("features_input")
    private List<String> featuresInput;

    @JsonProperty("internal_components")
    private Map<String, Map<String, Object>> internalComponents;
    

    /* EJEMPLO DE JSON
        {
        "arquitectura": "VotingClassifier",
        "estrategia_voto": "soft",
        "pesos_asignados": [2, 1, 1],
        "orden_estimadores": ["xgboost_champion", "lightgbm_opt", "rf_base"],
        "semilla_aleatoria": 42,
        "componentes_internos": {
            "xgboost_champion": {
            "objective": "binary:logistic",
            "n_estimators": 650,
            "learning_rate": 0.045,
            "max_depth": 6,
            "scale_pos_weight": 4.2,
            "subsample": 0.8,
            "colsample_bytree": 0.8
            },
            "lightgbm_opt": {
            "objective": "binary",
            "metric": "auc",
            "n_estimators": 500,
            "num_leaves": 31,
            "learning_rate": 0.05,
            "scale_pos_weight": 3.8
            },
            "rf_base": {
            "n_estimators": 100,
            "max_depth": 10,
            "class_weight": "balanced",
            "criterion": "gini"
            }
        },
        "features_input": [
            "PAY_0", "LIMIT_BAL", "EDAD", "BILL_AMT1", "PAY_AMT1"
        ]
        }

    */
}
