package com.naal.bankmind.dto.atm.projection;

import java.math.BigDecimal;

public interface WithdrawalAvgProjectionDTO {
    Long getIdAtm();
    BigDecimal getAvgWithdrawal();
}
