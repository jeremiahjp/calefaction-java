package com.calefaction.features.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Alert(
        String sender_name,
        String event,
        long start,
        long end,
        String description,
        List<String> tags) {
}
