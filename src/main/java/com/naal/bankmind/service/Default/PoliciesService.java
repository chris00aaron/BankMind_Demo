package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Request.PolicyRequestDTO;
import com.naal.bankmind.dto.Default.Response.DefaultPoliciesDTO;
import com.naal.bankmind.dto.Default.Response.DefaultPoliciesDTO.ClassificationRuleSBSDTO;
import com.naal.bankmind.entity.Default.DefaultPolicies;
import com.naal.bankmind.entity.Default.POJO.ClassficationRuleSBS;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de políticas de default.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoliciesService {

    private final DefaultPoliciesRepository policiesRepository;

    /**
     * Obtiene todas las políticas.
     */
    public List<DefaultPoliciesDTO> getAllPolicies() {
        return policiesRepository.findAllByOrderByActivationDateDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la política activa.
     */
    public DefaultPoliciesDTO getActivePolicy() {
        return policiesRepository.findByIsActiveTrue()
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Obtiene una política por ID.
     */
    public DefaultPoliciesDTO getPolicyById(Long id) {
        return policiesRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Crea una nueva política.
     */
    @Transactional
    public DefaultPoliciesDTO createPolicy(PolicyRequestDTO request) {
        DefaultPolicies policy = new DefaultPolicies();
        policy.setPolicyName(request.getPolicyName());
        policy.setThresholdApproval(request.getThresholdApproval());
        policy.setFactorLgd(request.getFactorLgd());
        policy.setDaysGraceDefault(request.getDaysGraceDefault());
        policy.setApprovedBy(request.getApprovedBy());
        policy.setActivationDate(LocalDate.now());
        policy.setIsActive(false);

        if (request.getSbsClassificationMatrix() != null) {
            List<ClassficationRuleSBS> rules = request.getSbsClassificationMatrix().stream()
                    .map(r -> {
                        ClassficationRuleSBS rule = new ClassficationRuleSBS();
                        rule.setCategoria(r.getCategoria());
                        rule.setMin(r.getMin());
                        rule.setMax(r.getMax());
                        rule.setProvision(r.getProvision());
                        return rule;
                    })
                    .collect(Collectors.toList());
            policy.setSbsClassificationMatrix(rules);
        }

        DefaultPolicies saved = policiesRepository.save(policy);
        log.info("Política creada: {} (ID: {})", saved.getPolicyName(), saved.getIdPolicy());
        return toDTO(saved);
    }

    /**
     * Actualiza una política existente.
     */
    @Transactional
    public DefaultPoliciesDTO updatePolicy(Long id, PolicyRequestDTO request) {
        DefaultPolicies policy = policiesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));

        if (request.getPolicyName() != null)
            policy.setPolicyName(request.getPolicyName());
        if (request.getThresholdApproval() != null)
            policy.setThresholdApproval(request.getThresholdApproval());
        if (request.getFactorLgd() != null)
            policy.setFactorLgd(request.getFactorLgd());
        if (request.getDaysGraceDefault() != null)
            policy.setDaysGraceDefault(request.getDaysGraceDefault());
        if (request.getApprovedBy() != null)
            policy.setApprovedBy(request.getApprovedBy());

        if (request.getSbsClassificationMatrix() != null) {
            List<ClassficationRuleSBS> rules = request.getSbsClassificationMatrix().stream()
                    .map(r -> {
                        ClassficationRuleSBS rule = new ClassficationRuleSBS();
                        rule.setCategoria(r.getCategoria());
                        rule.setMin(r.getMin());
                        rule.setMax(r.getMax());
                        rule.setProvision(r.getProvision());
                        return rule;
                    })
                    .collect(Collectors.toList());
            policy.setSbsClassificationMatrix(rules);
        }

        DefaultPolicies saved = policiesRepository.save(policy);
        log.info("Política actualizada: {} (ID: {})", saved.getPolicyName(), saved.getIdPolicy());
        return toDTO(saved);
    }

    /**
     * Activa una política (desactiva las demás).
     */
    @Transactional
    public DefaultPoliciesDTO activatePolicy(Long id) {
        // Desactivar todas
        List<DefaultPolicies> all = policiesRepository.findAll();
        for (DefaultPolicies p : all) {
            if (Boolean.TRUE.equals(p.getIsActive())) {
                p.setIsActive(false);
                p.setCancellationDate(LocalDate.now());
                policiesRepository.save(p);
            }
        }

        // Activar la seleccionada
        DefaultPolicies policy = policiesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Política no encontrada: " + id));
        policy.setIsActive(true);
        policy.setActivationDate(LocalDate.now());
        policy.setCancellationDate(null);

        DefaultPolicies saved = policiesRepository.save(policy);
        log.info("Política activada: {} (ID: {})", saved.getPolicyName(), saved.getIdPolicy());
        return toDTO(saved);
    }

    /**
     * Convierte entidad a DTO.
     */
    private DefaultPoliciesDTO toDTO(DefaultPolicies entity) {
        DefaultPoliciesDTO dto = new DefaultPoliciesDTO();
        dto.setIdPolicy(entity.getIdPolicy());
        dto.setPolicyName(entity.getPolicyName());
        dto.setThresholdApproval(entity.getThresholdApproval());
        dto.setFactorLgd(entity.getFactorLgd());
        dto.setDaysGraceDefault(entity.getDaysGraceDefault());
        dto.setActivationDate(entity.getActivationDate());
        dto.setCancellationDate(entity.getCancellationDate());
        dto.setIsActive(entity.getIsActive());
        dto.setApprovedBy(entity.getApprovedBy());

        if (entity.getSbsClassificationMatrix() != null) {
            List<ClassificationRuleSBSDTO> rules = entity.getSbsClassificationMatrix().stream()
                    .map(r -> new ClassificationRuleSBSDTO(
                            r.getCategoria(),
                            r.getMin(),
                            r.getMax(),
                            r.getProvision()))
                    .collect(Collectors.toList());
            dto.setSbsClassificationMatrix(rules);
        }

        return dto;
    }
}
