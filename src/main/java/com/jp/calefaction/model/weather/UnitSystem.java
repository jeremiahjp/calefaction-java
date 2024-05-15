package com.jp.calefaction.model.weather;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UnitSystem {
    IMPERIAL("imperial", "°F", "mph"),
    METRIC("metric", "°C", "km/h"),
    INTERNATIONAL("international", "°K", "m/s");

    private final String name;
    private final String temperatureUnit;
    private final String speedUnit;

    public static UnitSystem fromString(String text) {
        for (UnitSystem unit : UnitSystem.values()) {
            if (unit.name.equalsIgnoreCase(text)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
