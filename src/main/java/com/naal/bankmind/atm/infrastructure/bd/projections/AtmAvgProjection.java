package com.naal.bankmind.atm.infrastructure.bd.projections;

import java.math.BigDecimal;

// Proyección de la media de las features para un ATM en un día y mes específico
// @function fn_atm_features_avg
public interface AtmAvgProjection {
   Long getIdAtm();
   Integer getLocationType();
   BigDecimal getAvgLag1();
   BigDecimal getAvgLag5();
   BigDecimal getAvgLag11();
   BigDecimal getAvgTendenciaLags();
   BigDecimal getAvgRatioFindeVsSemana();
   BigDecimal getAvgRetirosFindeAnterior();
   BigDecimal getAvgRetirosDomingoAnterior();
   Short getAvgDomingoBajo();
   Short getAvgCaidaReciente();
}
