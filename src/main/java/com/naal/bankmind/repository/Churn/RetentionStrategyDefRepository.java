package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.RetentionStrategyDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetentionStrategyDefRepository extends JpaRepository<RetentionStrategyDef, Long> {
    List<RetentionStrategyDef> findByIsActiveTrue();
}
