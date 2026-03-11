package com.naal.bankmind.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.infrastructure.bd.migration.MigrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AtmMigrationScheduler {

    private final MigrationService migrationService;

    //Se ejecuta todos los días a las 12:00 AM (medianoche)
    @Scheduled(cron = "0 0 0 * * *")
    public void ejecutarCadaMedianoche() {
        migrationService.ejecutarSync();
    }
}
