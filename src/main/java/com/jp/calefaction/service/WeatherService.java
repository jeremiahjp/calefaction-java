package com.jp.calefaction.service;

import com.google.maps.model.GeocodingResult;
import com.jp.calefaction.model.weather.WeatherData;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class WeatherService {

    private final WebClient webClient;

    private static final String weatherURI = "/onecall";

    private static String units = "standard";

    private final String API_KEY = System.getenv("OpenWeatherAPI");

    public WeatherService(@Qualifier("OpenWeather") WebClient webClient) {
        this.webClient = webClient;
    }

    // Cache this

    @Cacheable(value = "openweather_cache", key = "#cacheKey")
    public WeatherData getOpenWeatherFromCoordinates(GeocodingResult[] result, String unit, String cacheKey) {
        units = unit;
        log.info("Cache miss for location {} in openweather_cache", cacheKey);

        double lat = result[0].geometry.location.lat;
        double lng = result[0].geometry.location.lng;
        String address = result[0].formattedAddress;

        log.info("Fetching weather from openweatherapi");
        WeatherData model = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(weatherURI)
                        .queryParam("lat", lat)
                        .queryParam("lon", lng)
                        .queryParam("appid", API_KEY)
                        .queryParam("units", unit)
                        .build())
                .retrieve()
                .bodyToMono(WeatherData.class)
                .block();

        model.setAddress(address);
        model.setIndex(0);

        try {
            return updateCache(cacheKey, model);
            // return createEmbed(model);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("No weather returned");
            return null;
        }
    }

    @CachePut(value = "openweather_cache", key = "#cacheKey")
    public WeatherData updateCache(String cacheKey, WeatherData cachedObject) {
        log.info("Caching entry for {}", cacheKey);
        return cachedObject;
    }

    public EmbedCreateSpec createEmbed(WeatherData model, String timePeriod) {
        log.info(
                "http://openweathermap.org/img/w/{}.png",
                model.getCurrent().getWeather().get(0).getIcon());
        if (timePeriod.equals("hourly")) {
            return createHourlyEmbed(model);
        }
        return createDailyEmbed(model);
    }

    public EmbedCreateSpec createDailyEmbed(WeatherData model) {
        String unit = getTempUnit();
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        int index = model.getIndex();

        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";

        return EmbedCreateSpec.builder()
                .color(Color.BLUE)
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(
                                        model.getDaily().get(index).getDt()),
                                zoneId))
                        .toString())
                .description(descUrl)
                .addField("Summary", model.getDaily().get(index).getSummary(), false)
                .addField(
                        "Temperature",
                        String.valueOf(Math.round(
                                        model.getDaily().get(index).getTemp().getDay())) + " " + unit,
                        true)
                .addField(
                        "Feel",
                        String.valueOf(Math.round(model.getDaily()
                                        .get(index)
                                        .getFeels_like()
                                        .getDay()))
                                + " "
                                + unit,
                        true)
                .addField(
                        "Cloudiness",
                        String.valueOf(Math.round(model.getDaily().get(index).getClouds())) + "%",
                        true)
                .addField(
                        "UV Index",
                        String.valueOf(Math.round(model.getDaily().get(index).getUvi())),
                        true)
                .addField("Humidity", String.valueOf(model.getDaily().get(index).getHumidity()) + "%", true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getDaily().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createHourlyEmbed(WeatherData model) {
        String unit = getTempUnit();
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        int index = model.getIndex();

        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        Random rand = new Random();
        return EmbedCreateSpec.builder()
                .color(Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()))
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(
                                        model.getHourly().get(index).getDt()),
                                zoneId))
                        .toString())
                .description(descUrl)
                .addField(
                        "Summary",
                        model.getHourly().get(index).getWeather().get(0).getDescription(),
                        false)
                .addField(
                        "Temperature",
                        String.valueOf(Math.round(model.getHourly().get(index).getTemp())) + " " + unit,
                        true)
                .addField(
                        "Feel",
                        String.valueOf(Math.round(model.getHourly().get(index).getFeels_like())) + " " + unit,
                        true)
                .addField(
                        "Cloudiness",
                        String.valueOf(Math.round(model.getHourly().get(index).getClouds())) + "%",
                        true)
                .addField(
                        "UV Index",
                        String.valueOf(Math.round(model.getHourly().get(index).getUvi())),
                        true)
                .addField(
                        "Humidity", String.valueOf(model.getHourly().get(index).getHumidity()) + "%", true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getHourly().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createThreeDay(WeatherData model) {
        String unit = getTempUnit();
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        int index = model.getIndex();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE M/d/yyyy");
        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        Random rand = new Random();
        return EmbedCreateSpec.builder()
                .color(Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()))
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(
                                        model.getHourly().get(index).getDt()),
                                zoneId))
                        .toString())
                .description(descUrl)
                .addField(
                        "`" + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(0).getDt()),
                                        zoneId)
                                .format(format) + "`",
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(0).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getFeels_like().getEve())) + " " + unit,
                        false)
                .addField(
                        '`' + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(1).getDt()),
                                        zoneId)
                                .format(format) + '`',
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(1).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getFeels_like().getEve())) + " " + unit,
                        false)
                .addField(
                        '`' + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(2).getDt()),
                                        zoneId)
                                .format(format) + '`',
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(2).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getFeels_like().getEve())) + " " + unit,
                        false)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getHourly().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createFiveDayEmbed(WeatherData model) {
        String unit = getTempUnit();
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        int index = model.getIndex();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE M/d/yyyy");
        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        Random rand = new Random();
        return EmbedCreateSpec.builder()
                .color(Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()))
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(
                                        model.getHourly().get(index).getDt()),
                                zoneId))
                        .toString())
                .description(descUrl)
                .addField(
                        '`' + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(0).getDt()),
                                        zoneId)
                                .format(format) + '`',
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(0).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(0).getFeels_like().getEve())) + " " + unit,
                        false)
                .addField(
                        '`' + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(1).getDt()),
                                        zoneId)
                                .format(format) + '`',
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(1).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(1).getFeels_like().getEve())) + " " + unit,
                        false)
                .addField(
                        '`' + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(2).getDt()),
                                        zoneId)
                                .format(format) + '`',
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(2).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(2).getFeels_like().getEve())) + " " + unit,
                        false)
                .addField(
                        '`' + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(3).getDt()),
                                        zoneId)
                                .format(format) + '`',
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(3).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(3).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(3).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(3).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(3).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(3).getFeels_like().getEve())) + " " + unit,
                        false)
                .addField(
                        '`' + ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(4).getDt()),
                                        zoneId)
                                .format(format) + '`',
                        "High: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(4).getTemp().getMax())) + " " + unit + "\n"
                                + "Low: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(4).getTemp().getMin())) + " " + unit + "\n"
                                + "Chance of rain: "
                                + Math.round(model.getDaily().get(4).getRain()) * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(4).getFeels_like().getMorn())) + " " + unit + "\n"
                                + "Day real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(4).getFeels_like().getDay())) + " " + unit + "\n"
                                + "Eve real feel: "
                                + String.valueOf(Math.round(
                                        model.getDaily().get(4).getFeels_like().getEve())) + " " + unit,
                        false)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getHourly().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createCurrentEmbed(WeatherData model) {
        String unit = getTempUnit();
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm a z");

        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        Random rand = new Random();
        return EmbedCreateSpec.builder()
                .color(Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()))
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId))
                        .format(format))
                .description(descUrl)
                .addField("Summary", model.getCurrent().getWeather().get(0).getDescription(), false)
                .addField(
                        "Temperature",
                        String.valueOf(Math.round(model.getCurrent().getTemp())) + " " + unit,
                        true)
                .addField("Feel", String.valueOf(Math.round(model.getCurrent().getFeels_like())) + " " + unit, true)
                .addField(
                        "Cloudiness",
                        String.valueOf(Math.round(model.getCurrent().getClouds())) + "%",
                        true)
                .addField(
                        "UV Index", String.valueOf(Math.round(model.getCurrent().getUvi())), true)
                .addField("Humidity", String.valueOf(model.getCurrent().getHumidity()) + "%", true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createAstronomyEmbed(WeatherData model) {
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        int index = model.getIndex();

        DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm a z");
        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        Random rand = new Random();
        return EmbedCreateSpec.builder()
                .color(Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()))
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                        .format(format)))
                .description(descUrl)
                .addField(
                        "Sunrise",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(model.getCurrent().getSunrise()), zoneId)
                                .format(format),
                        true)
                .addField(
                        "Sunset",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(model.getCurrent().getSunset()), zoneId)
                                .format(format),
                        true)
                .addField(
                        "Moonrise",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(index).getMoonrise()),
                                        zoneId)
                                .format(format),
                        true)
                .addField(
                        "Moonset",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(0).getMoonset()),
                                        zoneId)
                                .format(format),
                        true)
                .addField(
                        "Moon Phase",
                        translateMoonPhase(model.getDaily().get(index).getMoon_phase()),
                        true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createAirQualityEmbed(WeatherData model) {
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        int index = model.getIndex();

        DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm a z");
        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        Random rand = new Random();
        return EmbedCreateSpec.builder()
                .color(Color.of(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()))
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                        .format(format)))
                .description(descUrl)
                .addField(
                        "Sunrise",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(model.getCurrent().getSunrise()), zoneId)
                                .format(format),
                        true)
                .addField(
                        "Sunset",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(model.getCurrent().getSunset()), zoneId)
                                .format(format),
                        true)
                .addField(
                        "Moonrise",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(index).getMoonrise()),
                                        zoneId)
                                .format(format),
                        true)
                .addField(
                        "Moonset",
                        ZonedDateTime.ofInstant(
                                        Instant.ofEpochSecond(
                                                model.getDaily().get(0).getMoonset()),
                                        zoneId)
                                .format(format),
                        true)
                .addField(
                        "Moon Phase",
                        translateMoonPhase(model.getDaily().get(index).getMoon_phase()),
                        true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
          
    }

    public String translateMoonPhase(double moonPhase) {
        if (moonPhase == 0 || moonPhase == 1) {
            return "new moon";
        }
        if (moonPhase == 0.25) {
            return "first quarter moon";
        }
        if (moonPhase >= 0.5) {
            return "full moon";
        }
        if (moonPhase == 0.75) {
            return "last quarter moon";
        }
        if (moonPhase > 0 && moonPhase < 0.25) {
            return "waxing crescent";
        }
        if (moonPhase > 0.25 && moonPhase < 0.5) {
            return "waxing gibous";
        }
        if (moonPhase > 0.5 && moonPhase < 0.75) {
            return "waning gibous";
        }
        if (moonPhase > 0.75 && moonPhase < 1) {
            return "waning crescent";
        } else {
            return "unknown";
        }
    }

    public boolean isFreedomUnits() {
        return units.equalsIgnoreCase("imperial");
    }

    public String getTempUnit() {
        String unit =
                switch (units) {
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

    public EmbedCreateSpec createAlertsEmbed(WeatherData model) {
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE M/d/yyyy '@' h:mm a z");
        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        return EmbedCreateSpec.builder()
                .color(Color.RED)
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                        .format(format)))
                .description(descUrl)
                .addField(
                        model.getAlerts().get(0).getEvent(),
                        "Started on `" + ZonedDateTime.ofInstant(Instant.ofEpochSecond(model.getAlerts().get(0).getStart()), zoneId).format(format) + "`\n"
                        + "Expires on `" + ZonedDateTime.ofInstant(Instant.ofEpochSecond(model.getAlerts().get(0).getEnd()), zoneId).format(format) + "`\n\n"
                        + model.getAlerts().get(0).getDescription(),
                        true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createVerboseEmbed(WeatherData model) {
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm a z");
        String descUrl = "[Weather in "
                + model.getAddress()
                + "]"
                + "(https://www.google.com/maps/@"
                + model.getLat()
                + ","
                + model.getLon()
                + ",13z)";
        return EmbedCreateSpec.builder()
                .color(Color.RED)
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                        .format(format)))
                .description(descUrl)
                .addField(
                        model.getAlerts().get(0).getEvent(),
                        "Started on `" + model.getAlerts().get(0).getStart() + "\n`"
                        + "Expires on `" + model.getAlerts().get(0).getEnd() + "\n`"
                        + model.getAlerts().get(0).getDescription(),
                        true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }
}
