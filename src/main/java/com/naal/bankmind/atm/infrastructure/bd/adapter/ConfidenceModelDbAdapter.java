package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.ConfidenceModel;
import com.naal.bankmind.atm.domain.ports.out.repository.ConfidenceModelRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaWithdrawalModelRepository;
import com.naal.bankmind.atm.infrastructure.mapper.WithdrawalModelMapper;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class ConfidenceModelDbAdapter implements ConfidenceModelRepository {

    private JpaWithdrawalModelRepository jpaWithdrawalModelRepository;

    @Override
    public Optional<ConfidenceModel> obtenerConfidenceModelActivo() {
        return jpaWithdrawalModelRepository.findByIsActiveTrue()
            .map(WithdrawalModelMapper::toConfidenceModel);
    }
}
