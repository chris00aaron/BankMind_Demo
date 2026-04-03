package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.Optional;

import com.naal.bankmind.atm.domain.model.ModelPrediction;

public interface ModelPredictionRepository {

    /***
     * Busca el modelo actual en producción
     * @return La metadata del modelo actual en producción
     */
    Optional<ModelPrediction> buscarModeloActualEnProduccion();
}
