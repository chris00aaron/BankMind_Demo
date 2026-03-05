package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class RegistroAutoentrenamiento {
    Long id;
    String modelName;
    LocalDateTime startTraining;
    LocalDateTime endTraining;
    Integer trainingDurationMinutes;
    BigDecimal mae;
    BigDecimal mape;
    BigDecimal rmse;
    Map<String, Object> hyperparameters;
    Boolean isProduction;
    DatasetDetails dataset;

    public RegistroAutoentrenamiento(Long id, String modelName, LocalDateTime startTraining, LocalDateTime endTraining, 
        Integer trainingDurationMinutes, BigDecimal mae, BigDecimal mape, BigDecimal rmse, Boolean isProduction, 
        Map<String, Object> hyperparameters, DatasetDetails dataset) {
        
        this.id = id;
        this.modelName = modelName;
        this.startTraining = startTraining;
        this.endTraining = endTraining;
        this.trainingDurationMinutes = trainingDurationMinutes;
        this.mae = mae;
        this.mape = mape;
        this.rmse = rmse;
        this.isProduction = isProduction;
        this.hyperparameters = hyperparameters;
        this.dataset = dataset;
    }

    public RegistroAutoentrenamiento(Long id, String modelName, LocalDateTime startTraining, LocalDateTime endTraining,
            Integer trainingDurationMinutes, BigDecimal mae, BigDecimal mape, BigDecimal rmse, Boolean isProduction,
            Map<String, Object> hyperparameters) {
        this.id = id;
        this.modelName = modelName;
        this.startTraining = startTraining;
        this.endTraining = endTraining;
        this.trainingDurationMinutes = trainingDurationMinutes;
        this.mae = mae;
        this.mape = mape;
        this.rmse = rmse;
        this.isProduction = isProduction;
        this.hyperparameters = hyperparameters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public LocalDateTime getStartTraining() {
        return startTraining;
    }

    public void setStartTraining(LocalDateTime startTraining) {
        this.startTraining = startTraining;
    }

    public LocalDateTime getEndTraining() {
        return endTraining;
    }

    public void setEndTraining(LocalDateTime endTraining) {
        this.endTraining = endTraining;
    }

    public Integer getTrainingDurationMinutes() {
        return trainingDurationMinutes;
    }

    public void setTrainingDurationMinutes(Integer trainingDurationMinutes) {
        this.trainingDurationMinutes = trainingDurationMinutes;
    }

    public BigDecimal getMae() {
        return mae;
    }

    public void setMae(BigDecimal mae) {
        this.mae = mae;
    }

    public BigDecimal getMape() {
        return mape;
    }

    public void setMape(BigDecimal mape) {
        this.mape = mape;
    }

    public BigDecimal getRmse() {
        return rmse;
    }

    public void setRmse(BigDecimal rmse) {
        this.rmse = rmse;
    }

    public Boolean getIsProduction() {
        return isProduction;
    }

    public void setIsProduction(Boolean isProduction) {
        this.isProduction = isProduction;
    }

    public DatasetDetails getDataset() {
        return dataset;
    }

    public void setDataset(DatasetDetails dataset) {
        this.dataset = dataset;
    }

    public Map<String, Object> getHyperparameters() {
        return hyperparameters;
    }

    public void setHyperparameters(Map<String, Object> hyperparameters) {
        this.hyperparameters = hyperparameters;
    }
}
