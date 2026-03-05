package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.RegistroAutoentrenamiento;
import com.naal.bankmind.atm.domain.ports.out.repository.RegistroAutoentrenamientoRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaSelfTrainingAuditWithdrawalModelRepository;
import com.naal.bankmind.atm.infrastructure.mapper.SelfTrainingAuditWithdrawalModelMapper;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class RegistroAutoentrenamientoDbAdapter  implements RegistroAutoentrenamientoRepository{

    private final JpaSelfTrainingAuditWithdrawalModelRepository jpaSelfTrainingAuditWithdrawalModelRepository;

    @Override
    public Optional<RegistroAutoentrenamiento> findById(Long id) {
        return jpaSelfTrainingAuditWithdrawalModelRepository.buscarRegistroPorId(id)
                .map(SelfTrainingAuditWithdrawalModelMapper::toDomainWithDataset);
    }

    @Override
    public PageResult<RegistroAutoentrenamiento> findAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var pageResult = jpaSelfTrainingAuditWithdrawalModelRepository.findAll(pageable);
        var content = pageResult.getContent().stream().map(SelfTrainingAuditWithdrawalModelMapper::toDomainWithoutDataset).toList();

        return PageResult.of(content, pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.getSize(), pageResult.getNumber());
    }
}
