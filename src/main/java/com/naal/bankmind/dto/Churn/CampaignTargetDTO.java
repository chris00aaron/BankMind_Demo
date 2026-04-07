package com.naal.bankmind.dto.Churn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignTargetDTO {

    @JsonProperty("customerId")
    private Long customerId;

    @JsonProperty("customerName")
    private String customerName;

    private String status;

    @JsonProperty("contactDate")
    private String contactDate;

    @JsonProperty("responseDate")
    private String responseDate;
}
