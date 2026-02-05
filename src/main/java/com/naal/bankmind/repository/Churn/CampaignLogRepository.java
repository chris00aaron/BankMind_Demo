package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.CampaignLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignLogRepository extends JpaRepository<CampaignLog, Long> {
}
