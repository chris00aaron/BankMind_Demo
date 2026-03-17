package com.naal.bankmind.atm.infrastructure.bd.migration;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaSyncLogRepository;
import com.naal.bankmind.client.atm.SyntheticDataFeignClient;
import com.naal.bankmind.entity.atm.SyncLog;
import com.naal.bankmind.entity.atm.SyncLog.SyncStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class MigrationService {

    private final JpaSyncLogRepository syncLogRepository;
    private final SyntheticDataFeignClient syntheticDataFeignClient;

    private static final int UMBRAL_MINUTOS = 30;

    public void ejecutarSync() {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDate fechaObjetivo = LocalDate.now().minusDays(1);
        log.info("Iniciando ejecucion del sync para la fecha: {}", fechaObjetivo);

        if (!procesoPuedeEjecutarse(fechaObjetivo)) {
            return;
        }

        try {
            var response = syntheticDataFeignClient.generatedNewData(fechaObjetivo.toString());
            log.info("Respuesta del sync: {}", response);
        } catch (Exception e) {
            log.error("Error al ejecutar la sincronizacion de datos: {}", e.getMessage());
        } finally {
            log.info("Fin del sync, duracion: {} segundos", Duration.between(inicio, LocalDateTime.now()).toSeconds());
        }
    }

    private boolean procesoPuedeEjecutarse(LocalDate fechaObjetivo) {
        Optional<SyncLog> ultimoSyncLog = syncLogRepository.findTopByOrderBySyncStartDesc();

        if (ultimoSyncLog.isEmpty()) {
            return true;
        }

        SyncLog syncLog = ultimoSyncLog.get();

        if (syncLog.getStatus() != SyncStatus.IN_PROGRESS) {
            return true;
        }

        if (estaEnEjecucionLegitima(syncLog)) {
            log.info("Sincronizacion en progreso iniciada a las {}, omitiendo ejecucion", 
                syncLog.getSyncStart());
            return false;
        }

        // Si llega aqui, el proceso lleva mas de UMBRAL_MINUTOS y se considera muerto
        marcarProcesoComoMuerto(syncLog);
        return true;
    }

    /**
     * Un proceso se considera en ejecucion legitima si inicio hace menos de UMBRAL_MINUTOS
     */
    private boolean estaEnEjecucionLegitima(SyncLog syncLog) {
        LocalDateTime umbralTiempo = LocalDateTime.now().minusMinutes(UMBRAL_MINUTOS);
        return syncLog.getSyncStart().isAfter(umbralTiempo);
    }

    private void marcarProcesoComoMuerto(SyncLog syncLog) {
        log.warn("Proceso zombie detectado, iniciado a las {} y sin completarse en {} minutos. Marcando como FAILED",
            syncLog.getSyncStart(), UMBRAL_MINUTOS);
        syncLog.setStatus(SyncStatus.FAILED);
        syncLog.setErrorMessage(String.format(
            "Proceso zombie: sin completarse tras %d minutos desde las %s",
            UMBRAL_MINUTOS, syncLog.getSyncStart()
        ));
        syncLog.setSyncEnd(LocalDateTime.now());
        syncLogRepository.save(syncLog);
    }
}