package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.CampaignLog;
import com.naal.bankmind.entity.CampaignTarget;
import com.naal.bankmind.entity.CampaignTargetKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignTargetRepository extends JpaRepository<CampaignTarget, CampaignTargetKey> {

    List<CampaignTarget> findByIdIdCampaign(Long campaignId);

    @Query("SELECT COUNT(ct) FROM CampaignTarget ct WHERE ct.id.idCampaign = :campaignId AND ct.status = 'CONVERTED'")
    int countConvertedByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(ct) FROM CampaignTarget ct WHERE ct.status = 'CONVERTED'")
    int countAllConverted();

    /** Returns [campaignId, convertedCount] pairs for all campaigns in one query — avoids N+1. */
    @Query("SELECT ct.id.idCampaign, COUNT(ct) FROM CampaignTarget ct WHERE ct.status = 'CONVERTED' GROUP BY ct.id.idCampaign")
    List<Object[]> countConvertedGroupedByCampaign();

    /** Counts CONVERTED targets excluding a specific campaign by name — used for executive metrics. */
    @Query("SELECT COUNT(ct) FROM CampaignTarget ct WHERE ct.status = 'CONVERTED' AND ct.id.idCampaign NOT IN (SELECT cl.idCampaign FROM CampaignLog cl WHERE cl.name = :excludeName)")
    int countAllConvertedExcluding(@Param("excludeName") String excludeName);
}
