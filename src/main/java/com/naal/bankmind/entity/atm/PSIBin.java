package com.naal.bankmind.entity.atm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.naal.bankmind.atm.domain.model.PSIBinModel;

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
public class PSIBin implements PSIBinModel {
    List<Long> bins;
    PSIBinStats stats;

    @JsonProperty("expected_pct")
    List<Long> expectedPct;
}
