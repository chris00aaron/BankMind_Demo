package com.naal.bankmind.atm.domain.criteria_query;

import java.time.LocalDate;

public record SelfTrainingAuditCriteria(
    LocalDate fechaInicioEjecucion
) {}
