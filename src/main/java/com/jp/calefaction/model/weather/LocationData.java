package com.jp.calefaction.model.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LocationData {
    @JsonProperty("lat")
    private double lat;

    @JsonProperty("lng")
    private double lng;

    // @JsonProperty("address")
    // private String address;
}
