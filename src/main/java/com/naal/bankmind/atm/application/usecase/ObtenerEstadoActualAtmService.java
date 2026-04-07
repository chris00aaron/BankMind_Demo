package com.naal.bankmind.atm.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.EstadoAtmDTO;
import com.naal.bankmind.atm.application.mapper.AtmStatusBalanceMapper;
import com.naal.bankmind.atm.domain.ports.in.ObtenerEstadoActualAtmUseCase;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaAtmRepository;
import com.naal.bankmind.atm.infrastructure.mapper.AtmStatusBalanceProjectionMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObtenerEstadoActualAtmService implements ObtenerEstadoActualAtmUseCase {

    private final JpaAtmRepository jpaAtmRepository;

    @Override
    public List<EstadoAtmDTO> obtenerEstadoActualAtm() {
        LocalDate predictionDate = LocalDate.of(2025, 12, 02);

		var atms = jpaAtmRepository.obtenerEstadoYSaldoDeCajero(predictionDate);
		var status = atms.stream().map(AtmStatusBalanceProjectionMapper::toDomain).toList();
		return status.stream().map(AtmStatusBalanceMapper::toDto).toList();
    }

}
