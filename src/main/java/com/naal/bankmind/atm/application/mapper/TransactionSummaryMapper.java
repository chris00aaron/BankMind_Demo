package com.naal.bankmind.atm.application.mapper;

import com.naal.bankmind.atm.application.dto.response.TransactionSummaryDTO;
import com.naal.bankmind.atm.domain.model.TransactionSummary;

public class TransactionSummaryMapper {

    public static TransactionSummaryDTO toDTO(TransactionSummary transactionSummary) {
        return new TransactionSummaryDTO(
            transactionSummary.fechaTransaccion(),
            transactionSummary.retiroTotal(),
            transactionSummary.depositoTotal()
        );
    }

}
