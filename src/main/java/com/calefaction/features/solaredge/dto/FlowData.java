package com.calefaction.features.solaredge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlowData(String status, double currentPower, Double chargeLevel) {
}
