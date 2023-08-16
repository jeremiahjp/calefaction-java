package com.jp.calefaction.model.weather;

import lombok.Data;

@Data
public class CurrentWeatherModel {

    private Coord coord;
    private Weather[] weather;
    private String base;
    private Main main;
    private int visibility;
    private Wind wind;
    private Rain rain;
    private Clouds clouds;
    private String dt;
    private Sys sys;
    private String timezone;
    private String id;
    private String name;
    private String cod;

    @Data
    public static class Coord {
        private double lat;
        private double lon;
    }

    @Data
    public static class Weather {
        private String id;
        private String main;
        private String description;
        private String icon;
    }

    @Data
    public static class Main {
        private String temp;
        private String feelsLike;
        private String tempMin;
        private String tempMax;
        private String pressure;
        private String humidity;
        private String seaLevel;
        private String grndLevel;
    }

    @Data
    public static class Wind {
        private String speed;
        private String deg;
        private String gust;
    }

    @Data
    public static class Rain {
        private String oneHr;
    }

    @Data
    public static class Clouds {
        private String all;
    }

    @Data
    public static class Sys {
        private String type;
        private String id;
        private String country;
        private int sunrise;
        private int sunset;
    }
}
