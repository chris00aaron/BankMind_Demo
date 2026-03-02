package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Request.MonitoringPolicyRequestDTO;
import com.naal.bankmind.dto.Default.Response.MonitoringPolicyDTO;
import com.naal.bankmind.service.Default.MonitoringPolicyService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para gestión de políticas de monitoreo.
 * Endpoints bajo /api/monitoring-policy.
 */
@RestController
@RequestMapping("/api/morosidad/monitoring-policy")
@RequiredArgsConstructor
public class MonitoringPolicyController {

    private final MonitoringPolicyService monitoringPolicyService;

    /**
     * Lista todas las políticas de monitoreo.
     * GET /api/monitoring-policy
     */
    @GetMapping
    public ResponseEntity<List<MonitoringPolicyDTO>> getAllPolicies() {
        return ResponseEntity.ok(monitoringPolicyService.getAllPolicies());
    }

    /**
     * Obtiene la política de monitoreo activa.
     * GET /api/monitoring-policy/active
     */
    @GetMapping("/active")
    public ResponseEntity<MonitoringPolicyDTO> getActivePolicy() {
        MonitoringPolicyDTO active = monitoringPolicyService.getActivePolicy();
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    /**
     * Crea una nueva política de monitoreo.
     * POST /api/monitoring-policy
     */
    @PostMapping
    public ResponseEntity<MonitoringPolicyDTO> createPolicy(@RequestBody MonitoringPolicyRequestDTO request) {
        return ResponseEntity.ok(monitoringPolicyService.createPolicy(request));
    }

    /**
     * Activa una política de monitoreo (desactiva las demás).
     * PUT /api/monitoring-policy/{id}/activate
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<MonitoringPolicyDTO> activatePolicy(@PathVariable Long id) {
        return ResponseEntity.ok(monitoringPolicyService.activatePolicy(id));
    }
}
