package com.jp.calefaction.service;

import com.jp.calefaction.model.weather.UnitSystem;
import com.jp.calefaction.model.weather.WeatherData;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WeatherEmbedResponseService {

    //     private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE M/d/yyyy");
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm a z");
    private static final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE M/d/yyyy");

    @CachePut(value = "openweather_cache", key = "#cacheKey")
    public WeatherData updateCache(String cacheKey, WeatherData cachedObject) {
        log.info("Caching entry for {}", cacheKey);
        return cachedObject;
    }

    public EmbedCreateSpec createDailyEmbed(WeatherData model, String unit) {
        String degreesUnit = UnitSystem.valueOf(unit.toUpperCase()).getTemperatureUnit();
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
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId))
                        .format(format))
                .description(descUrl)
                .addField("Summary", model.getDaily().get(index).getSummary(), false)
                .addField(
                        "Temperature",
                        Math.round(model.getDaily().get(index).getTemp().getDay()) + degreesUnit,
                        true)
                .addField(
                        "Feel",
                        Math.round(model.getDaily().get(index).getFeels_like().getDay()) + degreesUnit,
                        true)
                .addField("Cloudiness", Math.round(model.getDaily().get(index).getClouds()) + "%", true)
                .addField("UV Index", Math.round(model.getDaily().get(index).getUvi()) + " of 11", true)
                .addField("Humidity", model.getDaily().get(index).getHumidity() + "%", true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getDaily().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createHourlyEmbed(WeatherData model, String unit) {
        String degreesUnit = UnitSystem.valueOf(unit.toUpperCase()).getTemperatureUnit();
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
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId))
                        .format(format))
                .description(descUrl)
                .addField(
                        "Summary",
                        model.getHourly().get(index).getWeather().get(0).getDescription(),
                        false)
                .addField("Temperature", Math.round(model.getHourly().get(index).getTemp()) + degreesUnit, true)
                .addField("Feel", Math.round(model.getHourly().get(index).getFeels_like()) + degreesUnit, true)
                .addField("Cloudiness", Math.round(model.getHourly().get(index).getClouds()) + "%", true)
                .addField("UV Index", Math.round(model.getHourly().get(index).getUvi()) + " of 11", true)
                .addField("Humidity", model.getHourly().get(index).getHumidity() + "%", true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getHourly().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createThreeDay(WeatherData model, String unit) {
        String degreesUnit = UnitSystem.valueOf(unit.toUpperCase()).getTemperatureUnit();
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
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                        .format(format)))
                .description(descUrl)
                .addField(
                        "`"
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(0).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + "`",
                        "High: "
                                + Math.round(model.getDaily().get(0).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(0).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(0).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(0).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(0).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(0).getFeels_like().getEve()) + degreesUnit,
                        false)
                .addField(
                        '`'
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(1).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + '`',
                        "High: "
                                + Math.round(model.getDaily().get(1).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(1).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(1).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(1).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(1).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(1).getFeels_like().getEve()) + degreesUnit,
                        false)
                .addField(
                        '`'
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(2).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + '`',
                        "High: "
                                + Math.round(model.getDaily().get(2).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(2).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(2).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(2).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(2).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(2).getFeels_like().getEve()) + degreesUnit,
                        false)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getHourly().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createFiveDayEmbed(WeatherData model, String unit) {
        String degreesUnit = UnitSystem.valueOf(unit.toUpperCase()).getTemperatureUnit();
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
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                        .format(format)))
                .description(descUrl)
                .addField(
                        '`'
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(0).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + '`',
                        "High: "
                                + Math.round(model.getDaily().get(0).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(0).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(0).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(0).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(0).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(0).getFeels_like().getEve()) + degreesUnit,
                        false)
                .addField(
                        '`'
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(1).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + '`',
                        "High: "
                                + Math.round(model.getDaily().get(1).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(1).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(1).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(1).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(1).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(1).getFeels_like().getEve()) + degreesUnit,
                        false)
                .addField(
                        '`'
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(2).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + '`',
                        "High: "
                                + Math.round(model.getDaily().get(2).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(2).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(2).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(2).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(2).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(2).getFeels_like().getEve()) + degreesUnit,
                        false)
                .addField(
                        '`'
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(3).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + '`',
                        "High: "
                                + Math.round(model.getDaily().get(3).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(3).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(3).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(3).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(3).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(3).getFeels_like().getEve()) + degreesUnit,
                        false)
                .addField(
                        '`'
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getDaily().get(4).getDt()),
                                                zoneId)
                                        .format(dayFormatter)
                                + '`',
                        "High: "
                                + Math.round(model.getDaily().get(4).getTemp().getMax()) + degreesUnit + "\n"
                                + "Low: "
                                + Math.round(model.getDaily().get(4).getTemp().getMin()) + degreesUnit + "\n"
                                + "Chance of rain: "
                                + model.getDaily().get(4).getPop() * 100 + "%" + "\n"
                                + "Morn real feel: "
                                + Math.round(
                                        model.getDaily().get(4).getFeels_like().getMorn()) + degreesUnit + "\n"
                                + "Day real feel: "
                                + Math.round(
                                        model.getDaily().get(4).getFeels_like().getDay()) + degreesUnit + "\n"
                                + "Eve real feel: "
                                + Math.round(
                                        model.getDaily().get(4).getFeels_like().getEve()) + degreesUnit,
                        false)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getHourly().get(index).getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createCurrentEmbed(WeatherData model, String unit) {
        String degreesUnit = UnitSystem.valueOf(unit.toUpperCase()).getTemperatureUnit();
        ZoneId zoneId = ZoneId.of(model.getTimezone());

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
                .addField("Temperature", Math.round(model.getCurrent().getTemp()) + degreesUnit, true)
                .addField("Feel", Math.round(model.getCurrent().getFeels_like()) + degreesUnit, true)
                .addField("Cloudiness", Math.round(model.getCurrent().getClouds()) + "%", true)
                .addField("UV Index", Math.round(model.getCurrent().getUvi()) + " of 11", true)
                .addField("Humidity", model.getCurrent().getHumidity() + "%", true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createAstronomyEmbed(WeatherData model) {
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
        // this can be done better
        if (model.getAlerts() == null || model.getAlerts().size() == 0) {
            return EmbedCreateSpec.builder()
                    .color(Color.RED)
                    .title((ZonedDateTime.ofInstant(
                                    Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                            .format(format)))
                    .description(descUrl)
                    .addField("No alerts", "No alerts", true)
                    .thumbnail("http://openweathermap.org/img/w/"
                            + model.getCurrent().getWeather().get(0).getIcon()
                            + ".png")
                    .footer("Google Geo API & OpenWeather API", "")
                    .build();
        }
        String description = model.getAlerts().get(0).getDescription();
        String startedOn = "Started on `"
                + ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getAlerts().get(0).getStart()), zoneId)
                        .format(format)
                + "`\n";
        String expiresOn = "Expires on `"
                + ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getAlerts().get(0).getEnd()), zoneId)
                        .format(format)
                + "`\n\n";
        String cutoffMessage = "\n...\nThe rest was removed because Discord imposes 1024 char limit";
        int BUFFER_SIZE = cutoffMessage.length();
        if (model.getAlerts().get(0).getDescription().length() > (1024 - startedOn.length() - expiresOn.length())) {
            description = description.substring(0, 1024 - startedOn.length() - expiresOn.length() - BUFFER_SIZE);
            description = description.concat(cutoffMessage);
        }
        return EmbedCreateSpec.builder()
                .color(Color.RED)
                .title((ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(model.getCurrent().getDt()), zoneId)
                        .format(format)))
                .description(descUrl)
                .addField(
                        model.getAlerts().get(0).getEvent(),
                        "Started on `"
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getAlerts().get(0).getStart()),
                                                zoneId)
                                        .format(format)
                                + "`\n"
                                + "Expires on `"
                                + ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(
                                                        model.getAlerts().get(0).getEnd()),
                                                zoneId)
                                        .format(format)
                                + "`\n\n"
                                + description,
                        true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .build();
    }

    public EmbedCreateSpec createVerboseEmbed(WeatherData model, String unit) {
        ZoneId zoneId = ZoneId.of(model.getTimezone());
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

    public EmbedCreateSpec createOverviewEmbed(WeatherData model, String unit) {
        int index = 0;
        ZoneId zoneId = ZoneId.of(model.getTimezone());
        String degreesUnit = UnitSystem.valueOf(unit.toUpperCase()).getTemperatureUnit();
        String speedUnit = UnitSystem.valueOf(unit.toUpperCase()).getSpeedUnit();
        String descUrl = "[Overview for "
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
                .addField("Feels like", Math.round(model.getCurrent().getFeels_like()) + degreesUnit, true)
                .addField(
                        "High / Low",
                        Math.round(model.getDaily().get(index).getTemp().getMax()) + degreesUnit + "/"
                                + Math.round(
                                        model.getDaily().get(index).getTemp().getMin()) + degreesUnit,
                        true)
                .addField("Wind ", Math.round(model.getCurrent().getWind_speed()) + speedUnit, true)
                .addField("Humidity", model.getCurrent().getHumidity() + "%", true)
                .addField("Dew Point", Math.round(model.getCurrent().getDew_point()) + degreesUnit, true)
                .addField("Atmospheric Pressure", model.getCurrent().getPressure() + " hPa", true)
                .addField("UV Index", Math.round(model.getCurrent().getUvi()) + " of 11", true)
                .addField("Visibility ", model.getCurrent().getVisibility() + "km", true)
                .addField(
                        "Moon Phase",
                        translateMoonPhase(model.getDaily().get(index).getMoon_phase()),
                        true)
                .thumbnail("http://openweathermap.org/img/w/"
                        + model.getCurrent().getWeather().get(0).getIcon()
                        + ".png")
                .footer("Google Geo API & OpenWeather API", "")
                .timestamp(Instant.now())
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

    public List<Button> updateEmbedButtons(ComponentInteractionEvent event) {
        return event.getMessage().get().getComponents().stream()
                .filter(component -> component instanceof ActionRow)
                .map(component -> (ActionRow) component)
                .flatMap(actionRow -> actionRow.getChildren().stream())
                .filter(item -> item instanceof Button)
                .map(item -> (Button) item)
                .map(button -> {
                    String[] parts = button.getCustomId().get().split(",");
                    String lastPart = parts[parts.length - 1];
                    if (!"refresh".equals(lastPart)) { // We don't want to disable the refresh button
                        return button.disabled(button.getCustomId().get().equals(event.getCustomId()));
                    }
                    return button;
                })
                .collect(Collectors.toList());
    }

    public List<Button> disableAllExceptRefresh(ComponentInteractionEvent event) {
        return event.getMessage().get().getComponents().stream()
                .filter(component -> component instanceof ActionRow)
                .map(component -> (ActionRow) component)
                .flatMap(actionRow -> actionRow.getChildren().stream())
                .filter(item -> item instanceof Button)
                .map(item -> (Button) item)
                .map(button -> {
                    String[] parts = button.getCustomId().get().split(",");
                    String lastPart = parts[parts.length - 1];
                    return button.disabled(!lastPart.equals("refresh"));
                })
                .collect(Collectors.toList());
    }

    public List<LayoutComponent> updateEmbedComponents(ComponentInteractionEvent event) {
        List<Button> updatedButtons = updateEmbedButtons(event);
        List<LayoutComponent> updatedComponents = new ArrayList<>();

        // Split the buttons into groups of up to 5 and add them to updatedComponents
        for (int i = 0; i < updatedButtons.size(); i += 5) {
            List<Button> actionRowButtons = updatedButtons.subList(i, Math.min(i + 5, updatedButtons.size()));
            updatedComponents.add(ActionRow.of(actionRowButtons));
        }

        return updatedComponents;
    }

    public List<LayoutComponent> disableEmbedComponents(ComponentInteractionEvent event) {
        List<Button> updatedButtons = disableAllExceptRefresh(event);
        List<LayoutComponent> updatedComponents = new ArrayList<>();

        // Split the buttons into groups of up to 5 and add them to updatedComponents
        for (int i = 0; i < updatedButtons.size(); i += 5) {
            List<Button> actionRowButtons = updatedButtons.subList(i, Math.min(i + 5, updatedButtons.size()));
            updatedComponents.add(ActionRow.of(actionRowButtons));
        }

        return updatedComponents;
    }
}
