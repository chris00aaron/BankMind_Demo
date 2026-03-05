package com.naal.bankmind.atm.domain.model;

import java.time.LocalDate;

public record DatasetDetails(
    Long countTotal,
    Long countTrain,
    Long countTest,
    LocalDate startDate,
    LocalDate endDate
) {}
