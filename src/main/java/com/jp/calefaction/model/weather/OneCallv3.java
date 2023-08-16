package com.jp.calefaction.model.weather;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OneCallv3 {

    private String address;
    private String lat;
    private String lon;
    private String timezone;
    @JsonProperty("timezone_offset")
    private String timezoneOffset;
    private Current current;
    private List<Minutely> minutely;
    private List<Hourly> hourly;
    private List<Daily> daily;
    private List<Alerts> alerts;

    @Data
    public static class Current {
        private long dt;
        private long sunrise;
        private long sunset;
        private double temp;
        @JsonProperty("feels_like")
        private double feelsLike;
        private long pressure;
        private int humidity;
        @JsonProperty("dew_point")
        private double dewPoint;
        private double uvi;
        private int clouds;
        private int visibility;
        @JsonProperty("wind_speed")
        private double windSpeed;
        @JsonProperty("wind_deg")
        private int windDeg;
        @JsonProperty("wind_gust")
        private double windGust;
        private List<Weather> weather;
    }

    @Data
    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;
    }

    @Data
    public static class Minutely {
        private long dt;
        private int precipitation;
    }

    @Data
    public static class Hourly {
        private String dt;
        private double temp;
        @JsonProperty("feels_like")
        private double feelsLike;
        private long pressure;
        private int humidity;
        @JsonProperty("dew_point")
        private double dewPoint;
        private double uvi;
        private int clouds;
        private int visibility;
        @JsonProperty("wind_speed")
        private double windSpeed;
        @JsonProperty("wind_deg")
        private int windDeg;
        @JsonProperty("wind_gust")
        private double windGust;
        private Weather[] weather;
        private double pop;
        private List<Daily> daily;
    }

    @Data
    public static class Daily {
        private String dt;
        private long sunrise;
        private long sunset;
        private long moonrise;
        private long moonset;
        @JsonProperty("moon_phase")
        private double moonPhase;
        private String summary;
        private Temp temp;
        @JsonProperty("feels_like")
        private FeelsLike feelsLike;
        private long pressure;
        private int humidity;
        @JsonProperty("dew_point")
        private double dewPoint;
        @JsonProperty("wind_speed")
        private double windSpeed;
        @JsonProperty("wind_deg")
        private int windDeg;
        @JsonProperty("wind_gust")
        private double windGust;
        private List<Weather> weather;
        private int clouds;
        private double pop;
        private double rain;
        private double uvi;
    }

    @Data
    public static class Alerts {
        @JsonProperty("sender_name")
        private String senderName;
        private String event;
        private long start;
        private long end;
        private String description;
        // private List<Tags> tags; 
    }

    // @Data
    // public static class Tags {

    // }

    @Data
    public static class Temp {
        private double day;
        private double min;
        private double max;
        private double night;
        private double eve;
        private double morn;
    }

    @Data
    public static class FeelsLike {
        private double day;
        private double night;
        private double eve;
        private double morn;
    }
    
}
