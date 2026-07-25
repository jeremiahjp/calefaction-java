package com.calefaction.features.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Current(
        long dt,
        long sunrise,
        long sunset,
        double temp,
        double feels_like,
        int pressure,
        int humidity,
        double uvi,
        int clouds,
        int visibility,
        double wind_speed,
        double dew_point,
        List<Weather> weather) {
}
