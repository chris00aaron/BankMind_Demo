package com.naal.bankmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;

@Data
@Embeddable
public class CampaignTargetKey implements Serializable {
    @Column(name = "id_campaign")
    private Long idCampaign;

    @Column(name = "id_customer")
    private Long idCustomer;
}
