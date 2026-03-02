package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Response.StrategyResponseDTO;
import com.naal.bankmind.dto.Default.Response.StrategyResponseDTO.*;
import com.naal.bankmind.service.Default.StrategyService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/morosidad/strategy")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;

    /**
     * GET /api/strategy/segments
     * Retorna el resumen general + resumen por segmentos de riesgo.
     */
    @GetMapping("/segments")
    public ResponseEntity<StrategyResponseDTO> getSegments() {
        StrategySummary resumen = strategyService.getGeneralSummary();
        List<SegmentSummary> segmentos = strategyService.getSegmentsSummary();
        return ResponseEntity.ok(new StrategyResponseDTO(resumen, segmentos));
    }

    /**
     * POST /api/strategy/simulate
     * Simula el impacto de una campaña sobre un segmento.
     * Body: { "campaignId": 1, "segment": "Alto" }
     */
    @PostMapping("/simulate")
    public ResponseEntity<SimulationResult> simulate(@RequestBody Map<String, Object> request) {
        Long campaignId = Long.valueOf(request.get("campaignId").toString());
        String segment = request.get("segment").toString();
        SimulationResult result = strategyService.simulateCampaign(campaignId, segment);
        return ResponseEntity.ok(result);
    }
}
