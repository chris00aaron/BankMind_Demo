package com.naal.bankmind.controller.Default;

import com.naal.bankmind.service.Default.SelfTrainingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para operaciones de auto-retraining del modelo de morosidad.
 */
@Slf4j
@RestController
@RequestMapping("/api/morosidad/self-training")
@RequiredArgsConstructor
public class SelfTrainingController {

    private final SelfTrainingService selfTrainingService;

    /**
     * Dispara el pipeline completo de auto-retraining.
     *
     * 1. Refresca la vista materializada (extracción de datos)
     * 2. Envía dataset a la API Python (entrenamiento)
     * 3. Guarda resultados en BD (training_history + dataset_info)
     *
     * POST /api/default/self-training/trigger
     * Query param opcional: optunaTrials (default 30)
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerRetraining(
            @RequestParam(value = "optunaTrials", required = false, defaultValue = "30") Integer optunaTrials) {

        log.info("📡 Solicitud de auto-retraining recibida. Optuna trials: {}", optunaTrials);

        Map<String, Object> result = selfTrainingService.ejecutarRetraining(optunaTrials);

        if (result.containsKey("error") && !result.containsKey("status")) {
            return ResponseEntity.badRequest().body(result);
        }

        if ("ERROR".equals(result.get("status"))) {
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }
}
