package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.Optional;

import com.naal.bankmind.atm.domain.model.ModelPrediction;

public interface ModelPredictionRepository {

    Optional<ModelPrediction> buscarModeloActualEnProduccion();
}
