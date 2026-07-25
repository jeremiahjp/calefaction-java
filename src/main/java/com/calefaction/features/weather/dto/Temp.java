package com.calefaction.features.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Temp(double day, double min, double max, double night, double eve, double morn) {
}
