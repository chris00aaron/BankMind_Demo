package com.naal.bankmind.atm.domain.ports.out.repository;

import com.naal.bankmind.atm.domain.model.ATMFeatureModel;
import com.naal.bankmind.atm.domain.model.PageResult;

public interface ATMFeatureModelRepository {

    PageResult<ATMFeatureModel> findAll(int page, int size);
}
