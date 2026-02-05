package com.naal.bankmind.service.atm;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.naal.bankmind.dto.atm.response.ModelProductionDTO;
import com.naal.bankmind.entity.atm.WithdrawalModel;
import com.naal.bankmind.repository.atm.WithdrawalModelRepository;
import com.naal.bankmind.utils.atm.ConfidenceModel;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@Service
public class WithdrawalModelService {

    private final WithdrawalModelRepository withdrawalModelRepository;

    public Optional<WithdrawalModel> obtenerModeloActual() {
        return withdrawalModelRepository.findByIsActiveTrue();
    }

    public Optional<ConfidenceModel> obtenerConfidenceModelActivo() {
        return this.obtenerModeloActual()
            .map(m -> new ConfidenceModel(
                m.getConfidenceLevel(),
                m.getImportancesFeatures(),
                m.getMargin()
            ));
    }

    //El error lo controlamos de forma interna
    public ModelProductionDTO obtenerModeloEnProduccion() {
        return this.obtenerModeloActual()
            .map(m -> new ModelProductionDTO(
                m.getSelfTrainingAudit().getModelName()+"_V"+m.getId() ,
                m.getSelfTrainingAudit().getMape(),
                m.getSelfTrainingAudit().getMae(),
                m.getSelfTrainingAudit().getRmse(),
                m.getStartDate()
            )).orElseThrow(() -> new RuntimeException("No se encontro un modelo en produccion"));
    }
}
