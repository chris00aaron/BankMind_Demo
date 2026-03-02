package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public record AtmStockPosteriorPrediccion(
    Long idAtm,
    String tipoUbicacion,
    String ubicacion,
    BigDecimal stockInicial,
    BigDecimal retiroPrevisto,
    BigDecimal stockFinal
) {
    // Constructor estático o "Factory Method"
    public static AtmStockPosteriorPrediccion of(AtmStock stock, BigDecimal retiroPrevisto) {
        BigDecimal retiro = (retiroPrevisto != null) ? retiroPrevisto : BigDecimal.ZERO;

        if(retiro.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El retiro previsto no puede ser negativo");
        }
        
        // Calculamos el stock final asegurando que no sea negativo
        BigDecimal calculado = stock.stock().subtract(retiro);
        BigDecimal stockFinal = calculado.compareTo(BigDecimal.ZERO) > 0 ? calculado : BigDecimal.ZERO;

        return new AtmStockPosteriorPrediccion(
            stock.idAtm(),
            stock.tipoUbicacion(),
            stock.ubicacion(),
            stock.stock(),
            retiro,
            stockFinal
        );
    }

    public boolean tieneRiesgoDeDesabastecimiento() {
        return stockFinal().compareTo(BigDecimal.ZERO) <= 0;
    }
}