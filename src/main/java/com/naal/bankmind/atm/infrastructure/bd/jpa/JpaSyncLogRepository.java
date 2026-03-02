package com.naal.bankmind.atm.infrastructure.bd.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.naal.bankmind.entity.atm.SyncLog;

public interface JpaSyncLogRepository extends JpaRepository<SyncLog, Long> {

    Optional<SyncLog> findTopByOrderBySyncStartDesc();

}
