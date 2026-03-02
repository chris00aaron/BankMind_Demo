package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.SelfTrainingAuditFraud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SelfTrainingAuditFraudRepository extends JpaRepository<SelfTrainingAuditFraud, Long> {

    /**
     * Encontrar el último entrenamiento
     */
    SelfTrainingAuditFraud findTopByOrderByEndTrainingDesc();

    /**
     * Encontrar entrenamientos exitosos recientes
     */
    List<SelfTrainingAuditFraud> findTop5ByPromotionStatusOrderByEndTrainingDesc(String status);
}
