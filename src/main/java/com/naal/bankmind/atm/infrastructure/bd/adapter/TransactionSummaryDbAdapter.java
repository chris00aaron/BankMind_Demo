package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.TransactionSummary;
import com.naal.bankmind.atm.domain.ports.out.repository.TransactionSummaryRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaDailyAtmTransactionRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class TransactionSummaryDbAdapter implements TransactionSummaryRepository {

    private final JpaDailyAtmTransactionRepository jpaDailyAtmTransactionRepository;

    @Override
    public List<TransactionSummary> obtenerResumenTransacciones(LocalDate desde, LocalDate hasta) {
        var result = jpaDailyAtmTransactionRepository.obtenerResumenTransacciones(desde, hasta);
        return result.stream().map(
            projection -> new TransactionSummary(
                projection.getTransactionDate(),
                projection.getWithdrawalTotal(),
                projection.getDepositTotal()
            )
        ).toList();
    }

}
