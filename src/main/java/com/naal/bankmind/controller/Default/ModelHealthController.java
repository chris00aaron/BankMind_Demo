package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Response.ModelHealthDTO;
import com.naal.bankmind.service.Default.ModelHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para monitoreo del modelo en producción.
 */
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModelHealthController {

    private final ModelHealthService modelHealthService;

    /**
     * Obtiene el estado del modelo en producción.
     */
    @GetMapping("/health")
    public ResponseEntity<ModelHealthDTO> getModelHealth() {
        ModelHealthDTO health = modelHealthService.getModelHealth();
        return ResponseEntity.ok(health);
    }
}
