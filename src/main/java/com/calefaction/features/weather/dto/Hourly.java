package com.calefaction.features.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Hourly(
        long dt,
        double temp,
        double feels_like,
        int pressure,
        int humidity,
        double uvi,
        int clouds,
        double wind_speed,
        double pop, // Probability of precipitation
        List<Weather> weather) {
}
