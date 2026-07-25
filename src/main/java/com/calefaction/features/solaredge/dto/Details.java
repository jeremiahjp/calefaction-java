package com.calefaction.features.solaredge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Details(
        String name,
        String status,
        String installationDate,
        double peakPower,
        Location location) {
}
