package com.calefaction.features.solaredge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DateValue(String date, Double value) {
}
