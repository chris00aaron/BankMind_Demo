package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.ModelPrediction;
import com.naal.bankmind.atm.domain.ports.out.repository.ModelPredictionRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaWithdrawalModelRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class ModelPredictionDbAdapter implements ModelPredictionRepository {

    private final JpaWithdrawalModelRepository jpaWithdrawalModelRepository;

    @Override
    public Optional<ModelPrediction> buscarModeloActualEnProduccion() {
        return this.jpaWithdrawalModelRepository.findByIsActiveTrue()
            .map(m -> new ModelPrediction(
                m.getSelfTrainingAudit().getModelName()+"_V"+m.getId(),
                m.getSelfTrainingAudit().getMape(),
                m.getSelfTrainingAudit().getMae(),
                m.getSelfTrainingAudit().getRmse(),
                m.getStartDate(),
                m.getEndDate()
            ));
    }

}
