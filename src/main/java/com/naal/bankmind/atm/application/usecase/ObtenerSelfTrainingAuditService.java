package com.naal.bankmind.atm.application.usecase;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDTO;
import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDetailsDTO;
import com.naal.bankmind.atm.application.dto.response.SelfTrainingAuditBaseDTO;
import com.naal.bankmind.atm.application.mapper.RegistroAutoentrenamientoMapper;
import com.naal.bankmind.atm.application.mapper.SelfTrainingAuditMapper;
import com.naal.bankmind.atm.domain.criteria_query.SelfTrainingAuditCriteria;
import com.naal.bankmind.atm.domain.exception.SelfTrainingAuditNotFoundException;
import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.RegistroAutoentrenamiento;
import com.naal.bankmind.atm.domain.model.SelfTrainingAudit;
import com.naal.bankmind.atm.domain.ports.in.ObtenerSelfTrainingAuditUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.RegistroAutoentrenamientoRepository;
import com.naal.bankmind.atm.domain.ports.out.repository.SelfTrainingAuditRepository;

import lombok.AllArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
@Transactional(readOnly = true)
public class ObtenerSelfTrainingAuditService implements ObtenerSelfTrainingAuditUseCase {

    private final RegistroAutoentrenamientoRepository registroAutoentrenamientoRepository;
    private final SelfTrainingAuditRepository selfTrainingAuditRepository;

    @Override
    public PageResult<RegistroAutoentrenamientoDTO> obtenerHistorialAutoentrenamiento(int page, int size) {
        PageResult<RegistroAutoentrenamiento> pageResult = registroAutoentrenamientoRepository.findAll(page, size);
        //Mapeamos el DTO
        var content = pageResult.content().stream().map(RegistroAutoentrenamientoMapper::toRegistroAutoentrenamientoDTO).toList();
        return PageResult.of(content, pageResult.page());
    }

    @Override
    public RegistroAutoentrenamientoDetailsDTO obtenerDetallePorId(Long id) {
        RegistroAutoentrenamiento model = registroAutoentrenamientoRepository.findById(id)
                .orElseThrow(() -> new SelfTrainingAuditNotFoundException(id));
        //Mapeamos el DTO
        return RegistroAutoentrenamientoMapper.toRegistroAutoentrenamientoDetailsDTO(model);
    }

    @Override
    public PageResult<SelfTrainingAuditBaseDTO> obtenerHistorialAutoentrenamiento(int page, int size,
            SelfTrainingAuditCriteria criteria) {
        PageResult<SelfTrainingAudit> pageResult = selfTrainingAuditRepository.buscarHistorial(page, size, criteria);
        //Mapeamos el DTO
        var content = pageResult.content().stream().map(SelfTrainingAuditMapper::toSelfTrainingAuditBaseDTO).toList();
        return PageResult.of(content, pageResult.page());
    }

}
