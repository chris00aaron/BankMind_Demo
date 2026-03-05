package com.naal.bankmind.atm.application.usecase;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.ModelProductionDTO;
import com.naal.bankmind.atm.application.mapper.ModelPredictionMapper;
import com.naal.bankmind.atm.domain.exception.ModeloPrediccionIndisponibleException;
import com.naal.bankmind.atm.domain.ports.in.ObtenerModeloActualEnProduccionUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.ModelPredictionRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ObtenerModeloActualService implements ObtenerModeloActualEnProduccionUseCase {

    private ModelPredictionRepository modelPredictionRepository;

    @Override
    public ModelProductionDTO getModeloActualEnProduccion() {
        return this.modelPredictionRepository.buscarModeloActualEnProduccion()
            .map(ModelPredictionMapper::toModelProductionDTO)
            .orElseThrow(() -> new ModeloPrediccionIndisponibleException("Actualmente no hay un modelo en produccion"));
    }
    
}
