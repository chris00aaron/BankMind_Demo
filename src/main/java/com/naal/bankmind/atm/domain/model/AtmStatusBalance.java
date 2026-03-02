package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AtmStatusBalance(
        Long idAtm,
        String direccion,
        String tipoLugar,
        BigDecimal capacidadMaxima,
        BigDecimal balanceActual) 
{
    
    public BigDecimal balanceActualPorcentual() {
        return balanceActual.divide(capacidadMaxima, 2, RoundingMode.HALF_UP);
    }

    public String analizaEstado() {
        // 0.15 = 15% // Critico
        // 0.40 = 50% // Alerta
        // 0.75 = 75% // Normal
        BigDecimal porcentaje = balanceActualPorcentual();
        if (porcentaje.compareTo(BigDecimal.valueOf(0.15)) < 0) { return "CRITICO";
        } else if (porcentaje.compareTo(BigDecimal.valueOf(0.50)) < 0) { return "ALERTA";
        } else { return "NORMAL";}
    }
}