package com.naal.bankmind.atm.infrastructure.bd.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.naal.bankmind.entity.atm.PerformanceMonitorModelAtm;

public interface JpaPerformanceMonitorModelAtmRepository extends JpaRepository<PerformanceMonitorModelAtm,Long>{

    Optional<PerformanceMonitorModelAtm> findTopByOrderByCreatedAtDesc();
}
