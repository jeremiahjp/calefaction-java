package com.jp.calefaction.model.weather;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UnitSystem {
    IMPERIAL("imperial", "°F", "mph"),
    METRIC("metric", "°C", "km/h"),
    INTERNATIONAL("international", "°C", "m/s");

    private final String name;
    private final String temperatureUnit;
    private final String speedUnit;
}
