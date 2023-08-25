package com.jp.calefaction.model.weather;

import java.util.List;
import lombok.Data;

@Data
public class WeatherData {
    private int index;
    private String address;
    private String unit;
    private double lat;
    private double lon;
    private String timezone;
    private int timezone_offset;
    private CurrentWeather current;
    private List<Minutely> minutely;
    private List<Hourly> hourly;
    private List<Daily> daily;
    private List<Alert> alerts;

    @Data
    public static class CurrentWeather {
        private long dt;
        private long sunrise;
        private long sunset;
        private double temp;
        private double feels_like;
        private int pressure;
        private int humidity;
        private double dew_point;
        private double uvi;
        private int clouds;
        private int visibility;
        private double wind_speed;
        private int wind_deg;
        private double wind_gust;
        private List<WeatherInfo> weather;
    }

    @Data
    public static class Minutely {
        private long dt;
        private int precipitation;
    }

    @Data
    public static class Hourly {
        private long dt;
        private double temp;
        private double feels_like;
        private int pressure;
        private int humidity;
        private double dew_point;
        private double uvi;
        private int clouds;
        private int visibility;
        private double wind_speed;
        private int wind_deg;
        private double wind_gust;
        private List<WeatherInfo> weather;
        private double pop;
    }

    @Data
    public static class Daily {
        private long dt;
        private long sunrise;
        private long sunset;
        private long moonrise;
        private long moonset;
        private double moon_phase;
        private String summary;
        private Temperature temp;
        private FeelsLike feels_like;
        private int pressure;
        private int humidity;
        private double dew_point;
        private double wind_speed;
        private int wind_deg;
        private double wind_gust;
        private List<WeatherInfo> weather;
        private int clouds;
        private double pop;
        private double rain;
        private double uvi;
    }

    @Data
    public static class Alert {
        private String sender_name;
        private String event;
        private long start;
        private long end;
        private String description;
        private List<String> tags;
    }

    @Data
    public static class WeatherInfo {
        private int id;
        private String main;
        private String description;
        private String icon;
    }

    @Data
    public static class Temperature {
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
