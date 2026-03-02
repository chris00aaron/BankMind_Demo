package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.domain.exception.ModeloPrediccionIndisponibleException;
import com.naal.bankmind.atm.domain.model.ImportanciaCaracteristicasML;
import com.naal.bankmind.atm.domain.ports.out.repository.ImportanciaCaracteristicasMLRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaWithdrawalModelRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class ImportanciaCaracteristicasMLDbAdapter implements ImportanciaCaracteristicasMLRepository{

    private JpaWithdrawalModelRepository jpaWithdrawalModelRepository;

    @Override
    public ImportanciaCaracteristicasML obtenerImportanciaCaracteristicasModeloActual() {
        
        Map<String, Object> importanciaFeatures = jpaWithdrawalModelRepository.findByIsActiveTrue()
            .map(model -> model.getImportancesFeatures())
            .orElseThrow(() -> new ModeloPrediccionIndisponibleException("No se encontró un modelo activo"));

        return new ImportanciaCaracteristicasML(importanciaFeatures);
    }
}
