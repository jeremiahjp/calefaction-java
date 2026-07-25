package com.calefaction.features.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FeelsLike(double day, double night, double eve, double morn) {
}
