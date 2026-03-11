package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.List;

import com.naal.bankmind.atm.domain.model.AtmStock;

public interface AtmStockRepository {

    /**
     * Obtiene el balance de stock actual de los cajeros
     * @return Lista de AtmStock
     */
    List<AtmStock> obtenerBalanceStockActual();
}
