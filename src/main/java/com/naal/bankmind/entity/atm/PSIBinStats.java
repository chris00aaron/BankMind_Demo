package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.naal.bankmind.atm.domain.model.PSIBinStatsModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PSIBinStats implements PSIBinStatsModel {
    private BigDecimal std;
    private BigDecimal mean;
    private BigDecimal median;

    @JsonProperty("null_pct")
    private BigDecimal nullPct;

    @JsonProperty("n_samples")
    private Long nSamples;
}
