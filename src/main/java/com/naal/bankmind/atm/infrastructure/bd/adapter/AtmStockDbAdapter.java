package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.AtmStock;
import com.naal.bankmind.atm.domain.ports.out.repository.AtmStockRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaAtmRepository;
import com.naal.bankmind.entity.atm.Atm;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class AtmStockDbAdapter implements AtmStockRepository {

    private final JpaAtmRepository jpaAtmRepository;

    @Override
    public List<AtmStock> obtenerBalanceStockActual() {

        List<Atm> atms = jpaAtmRepository.findActiveAtmsWithStatus();

        //Mapper to AtmStock
        return atms.stream()
                .map(atm -> new AtmStock(
                        atm.getIdAtm(),
                        atm.getLocationType().getDescription(),
                        atm.getAddress(),
                        atm.getCurrentStatus().getCurrentBalance()))
                .collect(Collectors.toList());
    }
}
