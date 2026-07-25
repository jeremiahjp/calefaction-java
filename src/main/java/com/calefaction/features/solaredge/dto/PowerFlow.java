package com.calefaction.features.solaredge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PowerFlow(
                String unit,
                List<Connection> connections,
                FlowData GRID,
                FlowData LOAD,
                FlowData PV,
                FlowData STORAGE) {
}
