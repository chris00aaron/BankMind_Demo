package com.naal.bankmind.atm.domain.ports.out.repository;

import java.time.LocalDate;
import java.util.List;

import com.naal.bankmind.atm.domain.model.TransactionSummary;

public interface TransactionSummaryRepository {

    /**
     * Obtiene el resumen de transacciones por fecha
     * @param desde Fecha inicial
     * @param hasta Fecha final
     * @return Lista de resúmenes de transacciones
     */
    List<TransactionSummary> obtenerResumenTransacciones(LocalDate desde, LocalDate hasta);
}
