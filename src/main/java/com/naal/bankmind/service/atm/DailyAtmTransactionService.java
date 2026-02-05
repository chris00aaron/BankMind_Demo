package com.naal.bankmind.service.atm;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.dto.atm.projection.WithdrawalAvgProjectionDTO;
import com.naal.bankmind.entity.atm.DailyAtmTransaction;
import com.naal.bankmind.entity.atm.TransactionType;
import com.naal.bankmind.repository.atm.DailyAtmTransactionRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class DailyAtmTransactionService {

    private final DailyAtmTransactionRepository dailyAtmTransactionRepository;

    public List<WithdrawalAvgProjectionDTO> obtenerRetirosHistoricos(short day, short month) {
        return dailyAtmTransactionRepository.obtenerRetiroDePromedioHistorico(day, month);
    }

    public List<DailyAtmTransaction> obtenerTransaccionesPorFecha(LocalDate fechaAyer) {
        return dailyAtmTransactionRepository.obtenerTransaccionesConDetallesParaPrediccion(fechaAyer, TransactionType.WITHDRAWAL);
    }
}
