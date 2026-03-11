package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.domain.model.ATMFeatureModel;
import com.naal.bankmind.atm.domain.model.DynamicFeatures;
import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.ports.out.repository.ATMFeatureModelRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaAtmFeaturesRepository;
import com.naal.bankmind.entity.atm.AtmFeatures;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class ATMFeatureModelDbAdapter implements ATMFeatureModelRepository {

    private final JpaAtmFeaturesRepository jpaAtmFeaturesRepository;

    @Override
    public PageResult<ATMFeatureModel> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AtmFeatures> pageResult = jpaAtmFeaturesRepository.findAllByOrderByReferenceDateDesc(pageable);

        List<ATMFeatureModel> atmFeatures = pageResult.getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        return PageResult.of(
            atmFeatures,
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.getSize(),
            pageResult.getNumber()
        );
    }

    private ATMFeatureModel toDomain(AtmFeatures atmFeatures) {
        return new ATMFeatureModel(
            atmFeatures.getIdFeatureStore(),
            atmFeatures.getTransaction().getIdTransaction(),
            atmFeatures.getReferenceDate(),
            atmFeatures.getDayOfMonth(),
            atmFeatures.getDayOfWeek(),
            atmFeatures.getMonth(),
            atmFeatures.getWithdrawalAmountDay(),
            atmFeatures.getCreatedAt(),
            DynamicFeatures.fromMap(atmFeatures.getDynamicFeatures())
        );
    }
}
