package com.naal.bankmind.atm.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.UltimoEstadoAtmDetailsUseDTO;
import com.naal.bankmind.atm.application.mapper.ATMActualStatusMapper;
import com.naal.bankmind.atm.domain.ports.in.ObtenerUltimoEstadoAtmDetailsUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.ATMActualStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObtenerUltimoEstadoAtmDetailsService implements ObtenerUltimoEstadoAtmDetailsUseCase {

    private final ATMActualStatusRepository atmActualStatusRepository;

    @Override
    public List<UltimoEstadoAtmDetailsUseDTO> obtenerUltimoEstadoAtmDetails() {
        var atmActualStatus = atmActualStatusRepository.listaCurrentStatus();
        return atmActualStatus.stream().map(ATMActualStatusMapper::toDto).toList();
    }
}
