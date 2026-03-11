package com.naal.bankmind.atm.application.usecase;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDTO;
import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDetailsDTO;
import com.naal.bankmind.atm.application.mapper.RegistroAutoentrenamientoMapper;
import com.naal.bankmind.atm.domain.exception.SelfTrainingAuditNotFoundException;
import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.RegistroAutoentrenamiento;
import com.naal.bankmind.atm.domain.ports.in.ObtenerSelfTrainingAuditUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.RegistroAutoentrenamientoRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ObtenerSelfTrainingAuditService implements ObtenerSelfTrainingAuditUseCase {

    private final RegistroAutoentrenamientoRepository registroAutoentrenamientoRepository;

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
        return RegistroAutoentrenamientoMapper.toRegistroAutoentrenamientoDetailsDTO(model);
    }

}
