package com.naal.bankmind.controller.Default;

import com.naal.bankmind.config.Default.MonthlyHistoryGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para operaciones de generación de datos de entrenamiento.
 */
@Slf4j
@RestController
@RequestMapping("/api/morosidad/data-generator")
@RequiredArgsConstructor
public class DataGeneratorController {

    private final MonthlyHistoryGenerator monthlyHistoryGenerator;

    /**
     * Genera datos sintéticos de monthly_history para probar auto-retraining.
     * 50% cuentas → 12 meses | 50% cuentas → 7 meses
     *
     * POST /api/default/generate-training-data
     */
    @PostMapping("/generate-training-data")
    public ResponseEntity<Map<String, Object>> generateTrainingData() {
        log.info("📡 Solicitud recibida: generar datos sintéticos de entrenamiento");

        try {
            Map<String, Object> result = monthlyHistoryGenerator.generarDatosSinteticos();
            log.info("✅ Generación completada: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error generando datos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
