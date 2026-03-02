package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Default.MitigationCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MitigationCampaignRepository extends JpaRepository<MitigationCampaign, Long> {

    List<MitigationCampaign> findByIsActiveTrue();

    @Query("SELECT c FROM MitigationCampaign c WHERE c.isActive = true AND (c.targetSegment = :segment OR c.targetSegment = 'Todos')")
    List<MitigationCampaign> findActiveBySegment(@Param("segment") String segment);
}
