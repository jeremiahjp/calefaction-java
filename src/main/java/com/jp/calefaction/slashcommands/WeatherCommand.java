package com.jp.calefaction.slashcommands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.jp.calefaction.model.weather.ButtonData;
import com.jp.calefaction.model.weather.LocationData;
import com.jp.calefaction.model.weather.WeatherData;
import com.jp.calefaction.service.GeoService;
import com.jp.calefaction.service.WeatherEmbedResponseService;
import com.jp.calefaction.service.WeatherService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import java.io.IOException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class WeatherCommand implements SlashCommand {

    private final GeoService geoService;
    private final WeatherService weatherService;
    private final WeatherEmbedResponseService embedResponseService;
    private final ObjectMapper jsonMapper;

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        String location = event.getOption("location")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        String unit = event.getOption("units")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();
        log.info("location {}, units {}", location, unit);

        if (!isValidLength(location)) {
            log.warn("Someone attempted to put an excessively long location");
            return event.reply("The provided location " + location + " is too long. Try and shorten it.")
                    .withEphemeral(true);
        }

        LocationData locData = new LocationData();
        GeocodingResult[] result;
        try {
            result = geoService.getGeoResults(location);
            if (result.length == 0) {
                log.info("Result from Geo API was empty");
                return event.reply("`" + location + " returned zero results" + "`")
                        .withEphemeral(true);
            }
            locData.setLat(result[0].geometry.location.lat);
            locData.setLng(result[0].geometry.location.lng);
            // locData.setAddress(result[0].formattedAddress);
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Had a problem reaching out to Geo api. Error: {}", e.getMessage());
            return event.reply("Had a problem reaching out to Geo api. Error: " + e.getMessage())
                    .withEphemeral(true);
        }

        ButtonData buttonData = new ButtonData();
        String buttonJson;
        try {

            String uuid = UUID.randomUUID().toString();
            buttonData.setCacheId(uuid);
            buttonData.setType("Overview");
            buttonJson = jsonMapper.writeValueAsString(buttonData);
            log.info("buttonjson {}", buttonJson);
            Button overview = Button.primary(buttonJson, "Overview").disabled(true);

            // We get the weather
            WeatherData weatherData =
                    weatherService.getOpenWeatherFromCoordinates(locData.getLat(), locData.getLng(), unit);
            weatherData.setAddress((result[0].formattedAddress));
            weatherService.updateCache(uuid, weatherData);

            buttonData.setCacheId(uuid);
            buttonData.setType("3-day");
            buttonJson = jsonMapper.writeValueAsString(buttonData);
            Button threeDay = Button.primary(buttonJson, "3-day");

            buttonData.setType("5-day");
            buttonJson = jsonMapper.writeValueAsString(buttonData);
            Button fiveDay = Button.primary(buttonJson, "5-day");

            buttonData.setType("Astronomy");
            buttonJson = jsonMapper.writeValueAsString(buttonData);
            Button astronomy = Button.primary(buttonJson, "Astronomy");

            buttonData.setType("Alerts");
            buttonJson = jsonMapper.writeValueAsString(buttonData);
            Button alerts = Button.danger(buttonJson, "Alerts");

            buttonData.setType("Hourly");
            buttonJson = jsonMapper.writeValueAsString(buttonData);
            Button hourly = Button.primary(buttonJson, "Hourly");

            return event.reply()
                    .withEmbeds(embedResponseService.createOverviewEmbed(weatherData, unit))
                    .withComponents(ActionRow.of(overview, threeDay, fiveDay), ActionRow.of(astronomy, alerts, hourly));
        } catch (JsonProcessingException e) {
            log.error("Failed mapping the button json. Button map: {}", buttonData);
            return event.reply("There was some error. It's been logged.").withEphemeral(true);
        }
    }

    private boolean isValidLength(String s) {
        return s.length() <= 80; // arbitrary size that should be good. need better logic here
    }
}
