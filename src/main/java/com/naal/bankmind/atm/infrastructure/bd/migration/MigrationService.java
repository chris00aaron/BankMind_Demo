package com.naal.bankmind.atm.infrastructure.bd.migration;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaSyncLogRepository;
import com.naal.bankmind.client.atm.SyntheticDataFeignClient;
import com.naal.bankmind.entity.atm.SyncLog;
import com.naal.bankmind.entity.atm.SyncLog.SyncStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationService {

    private final JpaSyncLogRepository syncLogRepository;
    private final SyntheticDataFeignClient syntheticDataFeignClient;

    //Representa los minutos maximos para considerar que el sync log aun es valido
    private final Integer UMBRAL_MINUTOS = 30;

    /**
     * Lanza el proceso de migracion de datos
     *
     * @param fechaObjetivo Fecha en formato YYYY-MM-DD
     */
    public void ejecutarSync() {
        
        if (!verificarEstadoValido()) {
            throw new RuntimeException("Ya se esta ejecutando una sincronizacion de datos");
        }

        try {
            log.info("Iniciando sincronizacion de datos");
            var response = syntheticDataFeignClient.generatedNewData(LocalDate.of(2026, 2, 3).toString());
            log.info("{}",response);
        } catch (Exception e) {
            log.error("Error al ejecutar la sincronizacion de datos", e);
            throw new RuntimeException("Error al ejecutar la sincronizacion de datos");
        }
    }

    private boolean verificarEstadoValido() {
        SyncLog ultimoSyncLog = syncLogRepository.findTopByOrderBySyncStartDesc().orElseThrow(() -> new RuntimeException("No se encontro un sync log"));
        
        SyncStatus status = ultimoSyncLog.getStatus();
        LocalDateTime syncStart = ultimoSyncLog.getSyncStart(); 
        LocalDateTime now = LocalDateTime.now();

        if (status != SyncStatus.IN_PROGRESS) {return true;}
    
        if (syncStart.isAfter(now.minusMinutes(UMBRAL_MINUTOS))) {return false;}
        else {
            notificarErrorEnSyncronizacionFueraDeTiempo(ultimoSyncLog);
            return true;
        }
    }

    private void notificarErrorEnSyncronizacionFueraDeTiempo(SyncLog syncLog) {
        syncLog.setStatus(SyncStatus.FAILED);
        syncLog.setErrorMessage("El sync log se encuentra fuera de tiempo");
        syncLog.setSyncEnd(LocalDateTime.now());
        syncLogRepository.save(syncLog);
    }
}

