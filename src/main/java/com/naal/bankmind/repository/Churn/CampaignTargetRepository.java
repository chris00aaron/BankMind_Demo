package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.CampaignTarget;
import com.naal.bankmind.entity.CampaignTargetKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignTargetRepository extends JpaRepository<CampaignTarget, CampaignTargetKey> {
}
