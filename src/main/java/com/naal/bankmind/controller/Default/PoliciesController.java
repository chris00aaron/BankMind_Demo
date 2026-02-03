package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Request.PolicyRequestDTO;
import com.naal.bankmind.dto.Default.Response.DefaultPoliciesDTO;
import com.naal.bankmind.service.Default.PoliciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestión de políticas de default.
 */
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PoliciesController {

    private final PoliciesService policiesService;

    /**
     * Lista todas las políticas.
     */
    @GetMapping
    public ResponseEntity<List<DefaultPoliciesDTO>> getAllPolicies() {
        return ResponseEntity.ok(policiesService.getAllPolicies());
    }

    /**
     * Obtiene la política activa.
     */
    @GetMapping("/active")
    public ResponseEntity<DefaultPoliciesDTO> getActivePolicy() {
        DefaultPoliciesDTO policy = policiesService.getActivePolicy();
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy);
    }

    /**
     * Obtiene una política por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DefaultPoliciesDTO> getPolicyById(@PathVariable Long id) {
        DefaultPoliciesDTO policy = policiesService.getPolicyById(id);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy);
    }

    /**
     * Crea una nueva política.
     */
    @PostMapping
    public ResponseEntity<DefaultPoliciesDTO> createPolicy(@RequestBody PolicyRequestDTO request) {
        DefaultPoliciesDTO created = policiesService.createPolicy(request);
        return ResponseEntity.ok(created);
    }

    /**
     * Actualiza una política existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DefaultPoliciesDTO> updatePolicy(
            @PathVariable Long id,
            @RequestBody PolicyRequestDTO request) {
        DefaultPoliciesDTO updated = policiesService.updatePolicy(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Activa una política (desactiva las demás).
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<DefaultPoliciesDTO> activatePolicy(@PathVariable Long id) {
        DefaultPoliciesDTO activated = policiesService.activatePolicy(id);
        return ResponseEntity.ok(activated);
    }
}
