package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Request.MonitoringPolicyRequestDTO;
import com.naal.bankmind.dto.Default.Response.MonitoringPolicyDTO;
import com.naal.bankmind.entity.Default.MonitoringPolicy;
import com.naal.bankmind.repository.Default.MonitoringPolicyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de políticas de monitoreo.
 * Sigue el mismo patrón de PoliciesService (create, list, activate).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringPolicyService {

    private final MonitoringPolicyRepository monitoringPolicyRepository;

    /**
     * Obtiene todas las políticas de monitoreo.
     */
    public List<MonitoringPolicyDTO> getAllPolicies() {
        return monitoringPolicyRepository.findAllByOrderByActivationDateDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la política de monitoreo activa.
     */
    public MonitoringPolicyDTO getActivePolicy() {
        return monitoringPolicyRepository.findByIsActiveTrue()
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Crea una nueva política de monitoreo (inactiva por defecto).
     */
    @Transactional
    public MonitoringPolicyDTO createPolicy(MonitoringPolicyRequestDTO request) {
        MonitoringPolicy policy = new MonitoringPolicy();
        policy.setPolicyName(request.getPolicyName());
        policy.setPsiThreshold(request.getPsiThreshold());
        policy.setConsecutiveDaysTrigger(request.getConsecutiveDaysTrigger());
        policy.setAucDropThreshold(request.getAucDropThreshold());
        policy.setKsDropThreshold(request.getKsDropThreshold());
        policy.setOptunaTrialsDrift(request.getOptunaTrialsDrift());
        policy.setOptunaTrialsValidation(request.getOptunaTrialsValidation());
        policy.setCreatedBy(request.getCreatedBy());
        policy.setActivationDate(LocalDate.now());
        policy.setIsActive(false);

        MonitoringPolicy saved = monitoringPolicyRepository.save(policy);
        log.info("Política de monitoreo creada: {} (ID: {})", saved.getPolicyName(), saved.getIdMonitoringPolicy());
        return toDTO(saved);
    }

    /**
     * Activa una política de monitoreo (desactiva las demás).
     */
    @Transactional
    public MonitoringPolicyDTO activatePolicy(Long id) {
        // Desactivar todas
        List<MonitoringPolicy> all = monitoringPolicyRepository.findAll();
        for (MonitoringPolicy p : all) {
            if (Boolean.TRUE.equals(p.getIsActive())) {
                p.setIsActive(false);
                p.setCancellationDate(LocalDate.now());
                monitoringPolicyRepository.save(p);
            }
        }

        // Activar la seleccionada
        MonitoringPolicy policy = monitoringPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política de monitoreo no encontrada: " + id));
        policy.setIsActive(true);
        policy.setActivationDate(LocalDate.now());
        policy.setCancellationDate(null);

        MonitoringPolicy saved = monitoringPolicyRepository.save(policy);
        log.info("Política de monitoreo activada: {} (ID: {})", saved.getPolicyName(), saved.getIdMonitoringPolicy());
        return toDTO(saved);
    }

    /**
     * Convierte entidad a DTO.
     */
    private MonitoringPolicyDTO toDTO(MonitoringPolicy entity) {
        MonitoringPolicyDTO dto = new MonitoringPolicyDTO();
        dto.setIdMonitoringPolicy(entity.getIdMonitoringPolicy());
        dto.setPolicyName(entity.getPolicyName());
        dto.setPsiThreshold(entity.getPsiThreshold());
        dto.setConsecutiveDaysTrigger(entity.getConsecutiveDaysTrigger());
        dto.setAucDropThreshold(entity.getAucDropThreshold());
        dto.setKsDropThreshold(entity.getKsDropThreshold());
        dto.setOptunaTrialsDrift(entity.getOptunaTrialsDrift());
        dto.setOptunaTrialsValidation(entity.getOptunaTrialsValidation());
        dto.setActivationDate(entity.getActivationDate());
        dto.setCancellationDate(entity.getCancellationDate());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
