package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Request.PredecirMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;
import com.naal.bankmind.service.Default.MorosidadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para predicción de morosidad.
 */
@RestController
@RequestMapping("/api/morosidad")
@RequiredArgsConstructor
public class MorosidadController {

    private final MorosidadService morosidadService;

    /**
     * Endpoint para predecir morosidad de una cuenta.
     * Recibe el recordId, arma el JSON internamente y devuelve la predicción.
     *
     * @param request DTO con recordId de la cuenta
     * @return Predicción de morosidad
     */
    @PostMapping("/predict")
    public ResponseEntity<MorosidadResponseDTO> predict(@Valid @RequestBody PredecirMorosidadRequestDTO request) {
        MorosidadResponseDTO response = morosidadService.predecirMorosidad(request);
        return ResponseEntity.ok(response);
    }
}
