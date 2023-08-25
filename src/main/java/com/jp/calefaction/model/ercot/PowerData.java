package com.jp.calefaction.model.ercot;

import java.util.Map;
import lombok.Data;

@Data
public class PowerData {
    private String lastUpdated;
    private String[] types;
    private Map<String, Map<String, PowerGeneration>> data;

    @Data
    public static class PowerGeneration {
        private double gen;
        private double hsl;
        private double maxCapacity;
        private int seasonalCapacity;
    }
}
