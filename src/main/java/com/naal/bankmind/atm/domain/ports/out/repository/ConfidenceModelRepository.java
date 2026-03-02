package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.Optional;

import com.naal.bankmind.atm.domain.model.ConfidenceModel;

public interface ConfidenceModelRepository {

    /**
     * Carga la metada del modelo predictivo de retiros de efectivo en cajeros automáticos
     * @return Metadata del modelo predictivo de retiros de efectivo en cajeros automáticos
     */
    Optional<ConfidenceModel> obtenerConfidenceModelActivo();
}
