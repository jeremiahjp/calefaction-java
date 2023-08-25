package com.jp.calefaction.commands;

import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.jp.calefaction.model.weather.WeatherData;
import com.jp.calefaction.service.EmbedResponseService;
import com.jp.calefaction.service.GeoService;
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
    private final EmbedResponseService embedResponseService;

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        var location = event.getOption("location")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        var unit = event.getOption("units")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();
        log.info("location {}, units {}", location, unit);
        GeocodingResult[] result;
        try {
            result = geoService.getGeoResults(location);
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Had a problem reaching out to Geo api. Error: {}", e.getMessage());
            return event.reply("There was a problem, try again in a few seconds");
        }

        if (result.length == 0) {
            log.info("Result from Geo API was empty");
            return event.reply("Something happened. Try again later");
        }

        long snowflake = event.getInteraction().getMember().get().getId().asLong();

        StringBuilder buttonString = new StringBuilder();
        buttonString.append(snowflake).append(",").append(location).append(",").append(unit);

        WeatherData weatherData = weatherService.getOpenWeatherFromCoordinates(result, unit, buttonString.toString());

        Button threeDay = Button.primary(buttonString + ",3-day", "3-day");
        Button fiveDay = Button.primary(buttonString + ",5-day", "5-day");
        Button current = Button.success(buttonString + ",current", "Current");
        Button overview = Button.primary(buttonString + ",overview", "Overview").disabled(true);
        Button astronomy = Button.primary(buttonString + ",astronomy", "Astronomy");
        // Button airQuality = Button.primary(buttonString + ",airquality", "Air Quality");
        Button alerts = Button.danger(buttonString + ",alerts", "Alerts");
        return event.reply()
                .withEmbeds(embedResponseService.createOverviewEmbed(weatherData, unit))
                .withComponents(ActionRow.of(threeDay, fiveDay, current), ActionRow.of(overview, astronomy, alerts));
    }
}
