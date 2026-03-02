package com.naal.bankmind.event;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.infrastructure.bd.migration.MigrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AtmModuleStartupRunner {

    private final MigrationService migrationService;

    //Se ejecuta al iniciar el modulo
    //@EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        migrationService.ejecutarSync();
    }
}
