package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.CampaignLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignLogRepository extends JpaRepository<CampaignLog, Long> {
    Optional<CampaignLog> findByName(String name);
}
