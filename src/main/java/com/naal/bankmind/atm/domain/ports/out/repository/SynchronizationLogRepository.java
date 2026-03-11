package com.naal.bankmind.atm.domain.ports.out.repository;


import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.SynchronizationLog;

public interface SynchronizationLogRepository {

    PageResult<SynchronizationLog> listarPaginado(int page, int size);

}
