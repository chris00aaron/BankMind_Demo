package com.naal.bankmind.atm.infrastructure.bd.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.atm.SelfTrainingAuditWithdrawalModel;

@Repository
public interface JpaSelfTrainingAuditWithdrawalModelRepository extends JpaRepository<SelfTrainingAuditWithdrawalModel, Long> {

    @Query("""
        SELECT s FROM SelfTrainingAuditWithdrawalModel s 
        JOIN FETCH s.dataset d
        WHERE s.id = :id
    """)
    Optional<SelfTrainingAuditWithdrawalModel> buscarRegistroPorId(Long id);
}
