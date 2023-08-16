package com.jp.calefaction.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.maps.model.GeocodingResult;
import com.jp.calefaction.model.weather.WeatherData;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class WeatherService {

    private final WebClient webClient;

    private static final String weatherURI = "/onecall";

    private static String units = "standard";

    private final String API_KEY = System.getenv("OpenWeatherAPI");

    public EmbedCreateSpec getOpenWeatherFromCoordinates(GeocodingResult[] result, String unit) {
        double lat = result[0].geometry.location.lat;
        double lng = result[0].geometry.location.lng;
        String address = result[0].formattedAddress;

        if (unit.equalsIgnoreCase("imperial") || unit.equalsIgnoreCase("f")) {
            units = "imperial";
        }

        if (unit.equalsIgnoreCase("metric") || unit.equalsIgnoreCase("c")) {
            units = "metric";
        }
        if (unit.equalsIgnoreCase("standard") || unit.equalsIgnoreCase("si")) {
            units = "standard";
        }

        log.info("Fetching weather from openweatherapi");
        WeatherData model = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(weatherURI)
                .queryParam("lat", lat)
                .queryParam("lon", lng)
                .queryParam("appid", API_KEY )
                .queryParam("units", units)
                .build())
            .retrieve()
            .bodyToMono(WeatherData.class)
            .block();

            model.setAddress(address);

            try {
                return createEmbed(model);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                log.error("No weather returned");
                return null;
            }
    }

    public EmbedCreateSpec createEmbed(WeatherData model) {
        String unit = getTempUnit();
        ZoneId zoneId = ZoneId.of(model.getTimezone());

        String descUrl = "[Weather in " + model.getAddress() + "]" + "(https://www.google.com/maps/@" + model.getLat() + "," + model.getLon() + ",13z)";
        return 
            EmbedCreateSpec.builder()
            .color(Color.BLUE)
            .title((ZonedDateTime.ofInstant(Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)).toString())
            .description(descUrl)
            .addField("Summary", model.getDaily().get(0).getSummary(), false)
            .addField("Temperature", String.valueOf(Math.round(model.getCurrent().getTemp())) + " " + unit, true)
            .addField("Feel", String.valueOf(Math.round(model.getCurrent().getFeels_like())) + " " + unit, true)
            .addField("Cloudiness", String.valueOf(Math.round(model.getCurrent().getClouds())) + "%", true)
            .addField("UV Index", String.valueOf(Math.round(model.getCurrent().getUvi())), true)
            .addField("Humidity", String.valueOf(model.getCurrent().getHumidity()) + "%", true)
            .thumbnail(String.format("http://openweathermap.org/img/w/${weatherData.weather[0].icon}.png", model.getCurrent().getWeather().get(0).getIcon()))
            .footer("Google Geo API & OpenWeather API", "")
            .build();
    }

    public boolean isFreedomUnits() {
        return units.equalsIgnoreCase("imperial");
    }

    public String getTempUnit() {
        String unit = switch (units) {
            case "imperial" -> {
                yield "F";
            }
            case "metric" -> {
                yield "C";
            }
            default -> {
                yield "K";
            }
        };
        return unit;
    }
}
