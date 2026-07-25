package com.calefaction.features.solaredge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnvBenefits(
        GasEmissionSaved gasEmissionSaved,
        double treesPlanted,
        double lightBulbs) {
}
