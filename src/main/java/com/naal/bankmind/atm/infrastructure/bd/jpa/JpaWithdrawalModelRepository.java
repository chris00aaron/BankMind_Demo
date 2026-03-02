package com.naal.bankmind.atm.infrastructure.bd.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.atm.WithdrawalModel;

@Repository
public interface JpaWithdrawalModelRepository extends JpaRepository<WithdrawalModel, Long> {
    public Optional<WithdrawalModel> findByIsActiveTrue();
}
