package com.naal.bankmind.service.atm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.naal.bankmind.dto.atm.response.DatasetDetailsDTO;
import com.naal.bankmind.dto.atm.response.RegistroAutoentrenamientoDTO;
import com.naal.bankmind.dto.atm.response.RegistroAutoentrenamientoDetailsDTO;
import com.naal.bankmind.entity.atm.SelfTrainingAuditWithdrawalModel;
import com.naal.bankmind.repository.atm.SelfTrainingAuditWithdrawalModelRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class SelfTrainingAuditWithdrawalModelService {

    private final SelfTrainingAuditWithdrawalModelRepository selfTrainingAuditWithdrawalModelRepository;

    
    public Page<RegistroAutoentrenamientoDTO> obtenerModelosEnProduccion(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return selfTrainingAuditWithdrawalModelRepository.findAll(pageable)
            .map(m -> new RegistroAutoentrenamientoDTO(
                m.getId(),
                m.getModelName(),
                m.getStartTraining(),
                m.getEndTraining(),
                m.getTrainingDurationMinutes(),
                m.getMae(),
                m.getMape(),
                m.getRmse(),
                m.getIsProduction()
            ));
    }

    public RegistroAutoentrenamientoDetailsDTO obtenerModeloPorId(Long id) {
        SelfTrainingAuditWithdrawalModel model = selfTrainingAuditWithdrawalModelRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registro de Autoentrenamiento no encontrado : " + id));
 
        return new RegistroAutoentrenamientoDetailsDTO(
            model.getId(),
            model.getModelName(),
            model.getStartTraining(),
            model.getEndTraining(),
            model.getTrainingDurationMinutes(),
            model.getMae(),
            model.getMape(),
            model.getRmse(),
            model.getHyperparameters(),
            model.getIsProduction(),
            new DatasetDetailsDTO(
                Long.parseLong(model.getDataset().getCountTotal()),
                Long.parseLong(model.getDataset().getCountTrain()),
                Long.parseLong(model.getDataset().getCountTest()),
                model.getDataset().getStartDate(),
                model.getDataset().getEndDate()
            )
        );
    }
}
