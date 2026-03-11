package com.naal.bankmind.atm.application.dto.response;

import java.util.List;

public record ResumenTransaccionDTO(
    List<TransactionSummaryDTO> resumen
) {}


