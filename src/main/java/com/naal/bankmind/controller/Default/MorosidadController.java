package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Request.PredecirMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.BatchPredictRequestDTO;
import com.naal.bankmind.dto.Default.Response.ClientePredictionDetailDTO;
import com.naal.bankmind.dto.Default.Response.ClientPaymentHistoryDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchAccountPredictionDTO;
import com.naal.bankmind.dto.Default.Response.BatchPredictionWrapperDTO;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;
import com.naal.bankmind.repository.Default.DefaultPredictionRepository;
import com.naal.bankmind.service.Default.MorosidadService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller para predicción de morosidad.
 */
@RestController
@RequestMapping("/api/morosidad")
@RequiredArgsConstructor
public class MorosidadController {

    private final MorosidadService morosidadService;
    private final DefaultPredictionRepository defaultPredictionRepository;

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
     * Acepta lista de recordIds y retorna predicciones con umbral de política y
     * resumen SHAP.
     */
    @PostMapping("/predict/batch")
    public ResponseEntity<BatchPredictionWrapperDTO> predictBatch(
            @RequestBody BatchPredictRequestDTO request) {
        // Delegar lógica completa al servicio
        boolean includeShap = request.getIncludeShap() != null ? request.getIncludeShap() : false;
        BatchPredictionWrapperDTO response = morosidadService.predecirBatch(request.getRecordIds(), includeShap);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate")
    public ResponseEntity<com.naal.bankmind.dto.Default.Response.SimulationResponseDTO> simulate(
            @RequestBody com.naal.bankmind.dto.Default.Request.SimulationRequestDTO request) {
        com.naal.bankmind.dto.Default.Response.SimulationResponseDTO response = morosidadService
                .simulatePrediction(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Timeline de predicciones para una cuenta.
     * Devuelve probabilidad de default + estado de pago real (payX) por fecha.
     * GET /api/morosidad/prediction-timeline/{recordId}
     */
    @GetMapping("/prediction-timeline/{recordId}")
    public ResponseEntity<List<Map<String, Object>>> getPredictionTimeline(
            @PathVariable Long recordId) {

        List<DefaultPrediction> predictions = defaultPredictionRepository
                .findByRecordIdOrderByDateAsc(recordId);

        // Agrupar por día, quedarse con la última predicción de cada día
        Map<String, Map<String, Object>> porDia = new LinkedHashMap<>();
        for (DefaultPrediction pred : predictions) {
            String dateKey = pred.getDatePrediction().toLocalDate().toString();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", dateKey);
            entry.put("defaultProbability", pred.getDefaultProbability());
            entry.put("defaultCategory", pred.getDefaultCategory());

            if (pred.getMonthlyHistory() != null) {
                Integer payX = pred.getMonthlyHistory().getPayX();
                entry.put("payX", payX != null ? payX : 0);
            } else {
                entry.put("payX", 0);
            }
            // Ordenado ASC → la última sobrescribe → queda la más reciente del día
            entry.put("mainRiskFactor", pred.getMainRiskFactor() != null ? pred.getMainRiskFactor() : "");
            porDia.put(dateKey, entry);
        }

        return ResponseEntity.ok(new ArrayList<>(porDia.values()));
    }

    /**
     * Historial de pagos mensual del cliente (máx. 10 meses).
     * GET /api/morosidad/payment-history/{recordId}
     */
    @GetMapping("/payment-history/{recordId}")
    public ResponseEntity<List<ClientPaymentHistoryDTO>> getPaymentHistory(
            @PathVariable Long recordId) {
        return ResponseEntity.ok(morosidadService.getPaymentHistory(recordId));
    }

    /**
     * Retorna la última predicción guardada para una cuenta sin recalcular.
     * Usado para cargar datos históricos desde el dashboard al hacer clic en un
     * cliente.
     * GET /api/morosidad/prediccion/{recordId}/ultima
     */
    @GetMapping("/prediccion/{recordId}/ultima")
    public ResponseEntity<ClientePredictionDetailDTO> getLastPrediction(
            @PathVariable Long recordId) {
        ClientePredictionDetailDTO result = morosidadService.getLastPredictionByRecordId(recordId);
        return ResponseEntity.ok(result);
    }
}
