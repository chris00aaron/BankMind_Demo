package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.ATMActualStatus;
import com.naal.bankmind.atm.domain.ports.out.repository.ATMActualStatusRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaAtmCurrentStatusRepository;
import com.naal.bankmind.atm.infrastructure.mapper.AtmCurrentStatusMapper;
import com.naal.bankmind.entity.atm.AtmCurrentStatus;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class ATMCurrentStatusDbAdapter implements ATMActualStatusRepository {

    private final JpaAtmCurrentStatusRepository jpaAtmCurrentStatusRepository;

    @Override
    public List<ATMActualStatus> listaCurrentStatus() {
        List<AtmCurrentStatus> atmCurrentStatus = jpaAtmCurrentStatusRepository.findAllCompleteWithAtmOrderByIdAtm();
        List<ATMActualStatus> atmActualStatus = atmCurrentStatus.stream()
                .map(AtmCurrentStatusMapper::toDomainWithAtm)
                .toList();

        return atmActualStatus;
    }
}