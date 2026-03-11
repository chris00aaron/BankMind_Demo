package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.naal.bankmind.atm.domain.model.PSIFeatureResultModel;

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
public class PSIFeatureResult implements PSIFeatureResultModel {
    private BigDecimal psi;
    private String alert;

    @JsonProperty("actual_pct")
    private BigDecimal[] actualPct;

    @JsonProperty("expected_pct")
    private BigDecimal[] expectedPct;

    @JsonProperty("prod_samples")
    private BigDecimal prodSamples;

    @JsonProperty("prod_null_pct")
    private BigDecimal prodNullPct;
}
