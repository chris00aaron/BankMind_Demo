package com.naal.bankmind.atm.infrastructure.bd.projections;

import java.math.BigDecimal;

//Esta es una proyeccion de una funcion de bd
public interface WithdrawalAvgProjection {
    Long getIdAtm();
    BigDecimal getAvgWithdrawal();
}
