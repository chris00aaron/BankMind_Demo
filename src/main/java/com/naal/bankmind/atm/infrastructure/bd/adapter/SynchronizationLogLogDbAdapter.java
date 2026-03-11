package com.naal.bankmind.atm.infrastructure.bd.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.SynchronizationLog;
import com.naal.bankmind.atm.domain.ports.out.repository.SynchronizationLogRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaSyncLogRepository;
import com.naal.bankmind.atm.infrastructure.mapper.SyncLogMapper;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class SynchronizationLogLogDbAdapter implements SynchronizationLogRepository {

    private final JpaSyncLogRepository jpaSyncLogRepository;

    @Override
    public PageResult<SynchronizationLog> listarPaginado(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "idSync"));
        var pageResult = jpaSyncLogRepository.findBySyncEndIsNotNull(pageable);
        var content = pageResult.getContent().stream().map(SyncLogMapper::toDomain).toList();

        return PageResult.of(content, pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.getSize(), pageResult.getNumber());
    }

}
