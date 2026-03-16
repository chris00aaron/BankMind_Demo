package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.criteria_query.SelfTrainingAuditCriteria;
import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.SelfTrainingAudit;
import com.naal.bankmind.atm.domain.ports.out.repository.SelfTrainingAuditRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaSelfTrainingAuditWithdrawalModelRepository;
import com.naal.bankmind.atm.infrastructure.mapper.SelfTrainingAuditWithdrawalModelMapper;
import com.naal.bankmind.entity.atm.SelfTrainingAuditWithdrawalModel;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class SelfTrainingAuditDbAdapter implements SelfTrainingAuditRepository {

    private final JpaSelfTrainingAuditWithdrawalModelRepository jpaSelfTrainingAuditWithdrawalModelRepository;
    
    @Override
    public PageResult<SelfTrainingAudit> buscarHistorial(int page, int size, SelfTrainingAuditCriteria criteria) {
        // Ordenamos por fecha de entrenamiento descendente por defecto para ver lo más reciente
        PageRequest pageable = PageRequest.of(page, size, Sort.by("startTraining").descending());

        // 1. Construimos la Especificación Dinámica de forma moderna
        // Si no hay fecha, la lambda devuelve null y Spring Data ignora el filtro
        Specification<SelfTrainingAuditWithdrawalModel> spec = (root, query, cb) -> {
            if (criteria.fechaInicioEjecucion() == null) {
                return null; 
            }

            // CORRECCIÓN TÉCNICA:
            // startTraining es LocalDateTime, así que comparamos con LocalDateTime.
            LocalDateTime inicioDelDia = criteria.fechaInicioEjecucion().atStartOfDay();
            LocalDateTime finDelDiaAhora = LocalDateTime.now().with(LocalTime.MAX);

            return cb.between(root.get("startTraining"), inicioDelDia, finDelDiaAhora);
        };

        // 2. Ejecutamos la consulta
        Page<SelfTrainingAuditWithdrawalModel> pageResult = jpaSelfTrainingAuditWithdrawalModelRepository.findAll(spec, pageable);

        // 3. Mapeo de Entidades a Dominio
        List<SelfTrainingAudit> content = pageResult.getContent().stream()
                .map(SelfTrainingAuditWithdrawalModelMapper::toDomain)
                .toList();

        // 4. Retorno usando tu objeto de página personalizado
        return PageResult.of(
            content, 
            pageResult.getTotalElements(), 
            pageResult.getTotalPages(), 
            pageResult.getSize(), 
            pageResult.getNumber()
        );
    }
}
