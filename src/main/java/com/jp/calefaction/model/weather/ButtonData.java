package com.jp.calefaction.model.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
// @JsonPropertyOrder({"snowflake", "location", "units", "buttonId"})
@JsonPropertyOrder({"cacheId, type"})
public class ButtonData {
    // @JsonProperty("snowflake")
    // private String snowflake;

    @JsonProperty("cacheId")
    private String cacheId;

    @JsonProperty("type")
    private String type;

    // @JsonProperty("location")
    // private String location;

    // @JsonProperty("lat")
    // private double lat;

    // @JsonProperty("lng")
    // private double lng;

    // @JsonProperty("units")
    // private String units;

    // @JsonProperty("buttonId")
    // private String buttonId;
}
