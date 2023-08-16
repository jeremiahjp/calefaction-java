package com.jp.calefaction.commands;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.jp.calefaction.service.GeoService;
import com.jp.calefaction.service.WeatherService;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class WeatherCommand implements SlashCommand {

    private final GeoService geoService;
    private final WeatherService weatherService;

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {         
        String address = event.getOption("location")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get();

        String unit = event.getOption("units")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get();

        String timePeriod = event.getOption("timeperiod")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get();


        GeocodingResult[] result;
        try {
            result = geoService.getGeoResults(address);
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Had a problem reaching out to Geo api. Error: {}", e.getMessage());
            return event.reply("There was a problem, try again in a few seconds");
        }

        if (result.length == 0) {
            log.info("Result from Geo API was empty");
            return event.reply("Something happened. Try again later");
        }

        EmbedCreateSpec embed = weatherService.getOpenWeatherFromCoordinates(result, unit);


        String buttonTitle = "day";
        if (timePeriod.equals("hourly")) {
            buttonTitle = "hour";
        }

        Button next = Button.success("next-btn", "next " + buttonTitle);
        Button prev = Button.success("previous-btn", "prev " + buttonTitle);
        return event.reply()
            .withEmbeds(embed)
            .withComponents(ActionRow.of(prev, next)); // just for fun
    }
}
