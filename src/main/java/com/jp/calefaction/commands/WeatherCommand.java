package com.jp.calefaction.commands;

import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
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
        GeocodingResult[] result;
        try {
            result = geoService.getGeoResults(location);
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Had a problem reaching out to Geo api. Error: {}", e.getMessage());
            return event.reply("Had a problem reaching out to Geo api. Error: " + e.getMessage());
        }

        if (result.length == 0) {
            log.info("Result from Geo API was empty");
            return event.reply("`" + location + " returned zero results" + "`");
        }

        long snowflake = event.getInteraction().getMember().get().getId().asLong();

        StringBuilder buttonString = new StringBuilder();
        buttonString.append(snowflake).append(",").append(location).append(",").append(unit);

        WeatherData weatherData = weatherService.getOpenWeatherFromCoordinates(result, unit, buttonString.toString());

        Button threeDay = Button.primary(buttonString + ",3-day", "3-day");
        Button fiveDay = Button.primary(buttonString + ",5-day", "5-day");
        Button overview = Button.primary(buttonString + ",overview", "Overview").disabled(true);
        Button astronomy = Button.primary(buttonString + ",astronomy", "Astronomy");
        // Button airQuality = Button.primary(buttonString + ",airquality", "Air Quality");
        Button alerts = Button.danger(buttonString + ",alerts", "Alerts");
        Button refresh = Button.primary(buttonString + ",refresh", "Refresh");
        return event.reply()
                .withEmbeds(embedResponseService.createOverviewEmbed(weatherData, unit))
                .withComponents(ActionRow.of(overview, threeDay, fiveDay), ActionRow.of(astronomy, alerts, refresh));
    }
}
