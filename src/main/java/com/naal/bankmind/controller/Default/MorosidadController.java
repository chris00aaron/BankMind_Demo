package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Request.PredecirMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.BatchPredictRequestDTO;
import com.naal.bankmind.dto.Default.Response.ClientePredictionDetailDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchAccountPredictionDTO;
import com.naal.bankmind.service.Default.MorosidadService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para predicción de morosidad.
 */
@RestController
@RequestMapping("/api/morosidad")
@RequiredArgsConstructor
public class MorosidadController {

    private final MorosidadService morosidadService;

    /**
     * Endpoint simple para predecir morosidad (retorna solo predicción).
     */
    @PostMapping("/predict")
    public ResponseEntity<MorosidadResponseDTO> predict(@RequestBody PredecirMorosidadRequestDTO request) {
        MorosidadResponseDTO response = morosidadService.predecirMorosidad(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint completo para predecir morosidad (retorna datos del cliente +
     * predicción).
     */
    @PostMapping("/predict/complete")
    public ResponseEntity<ClientePredictionDetailDTO> predictComplete(
            @RequestBody PredecirMorosidadRequestDTO request) {
        ClientePredictionDetailDTO response = morosidadService.predecirMorosidadCompleto(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint batch para predicción por lotes.
     * Acepta lista de recordIds y retorna predicciones para todas las cuentas.
     */
    @PostMapping("/predict/batch")
    public ResponseEntity<List<BatchAccountPredictionDTO>> predictBatch(
            @RequestBody BatchPredictRequestDTO request) {
        List<BatchAccountPredictionDTO> results = morosidadService.predecirBatch(request.getRecordIds());
        return ResponseEntity.ok(results);
    }
}
