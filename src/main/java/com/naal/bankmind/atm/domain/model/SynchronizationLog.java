package com.naal.bankmind.atm.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record SynchronizationLog(
    Long idSync,
    Integer recordsInserted,
    Integer recordsProcessed,
    Integer recordsUpdated,
    LocalDateTime syncStart,
    LocalDateTime syncEnd,
    String status,
    String sourceSystem,
    String errorMessage,
    List<ProcessLogStep> processLog
) {}