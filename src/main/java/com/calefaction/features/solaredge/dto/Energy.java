package com.calefaction.features.solaredge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Energy(String timeUnit, String unit, List<DateValue> values) {
}
