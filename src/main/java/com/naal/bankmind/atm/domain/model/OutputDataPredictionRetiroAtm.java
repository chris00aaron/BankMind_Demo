package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OutputDataPredictionRetiroAtm( Long atm, BigDecimal retiro, LocalDate predictionDate) {}