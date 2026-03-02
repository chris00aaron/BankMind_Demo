package com.naal.bankmind.atm.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.ports.in.GenerarPrediccionDiariaUseCase;
import com.naal.bankmind.atm.domain.ports.out.ai.WithdrawalPredictionIA;
import com.naal.bankmind.atm.domain.ports.out.repository.InputDataPredictionRetiroAtmRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@AllArgsConstructor
@Service
public class GenerarPrediccionDiariaService implements GenerarPrediccionDiariaUseCase {

    private final InputDataPredictionRetiroAtmRepository inputDataPredictionRetiroAtmRepository;

    private WithdrawalPredictionIA withdrawalPredictionService;

    @Override
    public List<OutputDataPredictionRetiroAtm> generarPrediccion(LocalDate fecha) {
        // 1. Obtener las transacciones del dia anterior
        List<InputDataPredictionRetiroAtm> inputs = inputDataPredictionRetiroAtmRepository
                .obtenerDataParaRealizarPrediccion(fecha);

        // 2. Realizar la predicción y retornarla
        return withdrawalPredictionService.predecirWithdrawalHistoric(inputs);
    }
}
