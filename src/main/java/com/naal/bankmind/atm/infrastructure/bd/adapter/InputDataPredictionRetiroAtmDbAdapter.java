package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.exception.DataInsuficienteParaPredecirException;
import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.ports.out.repository.InputDataPredictionRetiroAtmRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaDailyAtmTransactionRepository;
import com.naal.bankmind.atm.infrastructure.mapper.DailyAtmTransactionMapper;
import com.naal.bankmind.entity.atm.DailyAtmTransaction;
import com.naal.bankmind.entity.atm.TransactionType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class InputDataPredictionRetiroAtmDbAdapter implements InputDataPredictionRetiroAtmRepository {

    private final JpaDailyAtmTransactionRepository jpaDailyAtmTransactionRepository;

    @Override
    public List<InputDataPredictionRetiroAtm> obtenerDataParaRealizarPrediccion(LocalDate fecha) {

        // Para la prediccion se usa la data del dia anterior
        LocalDate fechaAyer = fecha.minusDays(1);

        // Obtenemos la data de las transacciones del dia anterior
        List<DailyAtmTransaction> transactions = jpaDailyAtmTransactionRepository
                .obtenerTransaccionesConDetallesParaPrediccion(fechaAyer, TransactionType.WITHDRAWAL);

        // Verificamos que por lo menos un registro para consultar a la api
        if (transactions.isEmpty())
            throw new DataInsuficienteParaPredecirException(fecha);

        // Generamos los Inputs
        return transactions.stream()
                .map(transaction -> {
                    var input = DailyAtmTransactionMapper.toInputDataPredictionRetiroAtm(transaction);
                    input.setFechaPrediccion(fecha); // Fecha para la cual se va a predecir
                    return input;
                })
                .toList();
    }
}
