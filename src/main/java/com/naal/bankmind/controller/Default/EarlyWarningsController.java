package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Response.EarlyWarningsPreviewDTO;
import com.naal.bankmind.service.Default.EarlyWarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para alertas tempranas.
 */
@RestController
@RequestMapping("/api/warnings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EarlyWarningsController {

    private final EarlyWarningsService warningsService;

    /**
     * Preview de alertas con umbrales temporales.
     * 
     * @param threshold Umbral de probabilidad de pago (0-100)
     * @param days      Días de anticipación (no usado aún, para futuro)
     */
    @GetMapping("/preview")
    public ResponseEntity<EarlyWarningsPreviewDTO> getWarningsPreview(
            @RequestParam(defaultValue = "30") double threshold,
            @RequestParam(defaultValue = "7") int days) {
        EarlyWarningsPreviewDTO preview = warningsService.getWarningsPreview(threshold, days);
        return ResponseEntity.ok(preview);
    }
}
