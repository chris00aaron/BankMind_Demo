package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.PerformanceMonitorModelAtmBase;
import com.naal.bankmind.atm.domain.ports.out.repository.PerformanceMonitorModelAtmBaseRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaPerformanceMonitorModelAtmRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class PerformanceMonitorModelAtmBaseDbAdapter implements PerformanceMonitorModelAtmBaseRepository{

    private final JpaPerformanceMonitorModelAtmRepository jpaPerformanceMonitorModelAtmRepository;

    @Override
    public Optional<PerformanceMonitorModelAtmBase> buscarUltimoRegistroMonitoreo() {
        return jpaPerformanceMonitorModelAtmRepository
                .findTopByOrderByCreatedAtDesc()
                .map(entity -> new PerformanceMonitorModelAtmBase(
                        entity.getId(),
                        entity.getPsiResults(),
                        entity.getMae(),
                        entity.getRmse(),
                        entity.getMape(),
                        entity.getDecision(),
                        entity.getMessage(),
                        entity.getAction(),
                        entity.getSummary(),
                        entity.getDetail(),
                        entity.getCreatedAt(),
                        entity.getNeedSelfTraining()
                ));
        
    }
}
