package com.calefaction.features.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherOneCallResponse(
        double lat,
        double lon,
        String timezone,
        int timezone_offset,
        Current current,
        List<Hourly> hourly,
        List<Daily> daily,
        List<Alert> alerts) {
}
