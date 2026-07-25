package com.calefaction.features.solaredge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Overview(
        CurrentPower currentPower,
        EnergyData lastDayData,
        EnergyData lastMonthData,
        EnergyData lastYearData,
        EnergyData lifeTimeData) {
}
