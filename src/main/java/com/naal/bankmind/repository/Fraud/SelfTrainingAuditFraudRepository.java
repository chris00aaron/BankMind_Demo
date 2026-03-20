package com.naal.bankmind.repository.Fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Fraud.SelfTrainingAuditFraud;

import java.util.List;

@Repository
public interface SelfTrainingAuditFraudRepository extends JpaRepository<SelfTrainingAuditFraud, Long> {

    /**
     * Encontrar el último entrenamiento
     */
    SelfTrainingAuditFraud findTopByOrderByEndTrainingDesc();

    /**
     * Encontrar el último entrenamiento EXITOSO (usado por el Sensor Reactivo de
     * Recall).
     */
    SelfTrainingAuditFraud findTopByIsSuccessTrueOrderByEndTrainingDesc();

    /**
     * Encontrar entrenamientos exitosos recientes
     */
    List<SelfTrainingAuditFraud> findTop5ByPromotionStatusOrderByEndTrainingDesc(String status);
}
