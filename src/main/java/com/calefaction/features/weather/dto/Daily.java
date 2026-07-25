package com.calefaction.features.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Daily(
        long dt,
        long sunrise,
        long sunset,
        Temp temp,
        FeelsLike feels_like,
        int pressure,
        int humidity,
        double wind_speed,
        List<Weather> weather,
        int clouds,
        double pop,
        double uvi,
        String summary) {
}
