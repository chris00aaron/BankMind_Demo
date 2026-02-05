package com.naal.bankmind.repository.atm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.atm.SelfTrainingAuditWithdrawalModel;

@Repository
public interface SelfTrainingAuditWithdrawalModelRepository extends JpaRepository<SelfTrainingAuditWithdrawalModel, Long> {
}
