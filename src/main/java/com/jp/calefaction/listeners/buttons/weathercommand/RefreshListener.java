package com.jp.calefaction.listeners.buttons.weathercommand;

import com.jp.calefaction.listeners.buttons.ButtonHandler;
import com.jp.calefaction.model.weather.WeatherData;
import com.jp.calefaction.service.GeoService;
import com.jp.calefaction.service.WeatherEmbedResponseService;
import com.jp.calefaction.service.WeatherService;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("Refresh")
@Slf4j
@AllArgsConstructor
public class RefreshListener implements ButtonHandler {

    private final WeatherEmbedResponseService embedResponseService;
    private final WeatherService weatherService;
    private final GeoService geoService;

    public String getCustomId(ButtonInteractionEvent event) {
        throw new UnsupportedOperationException("Unimplemented method 'getCustomId'");
    }

    public Mono<Void> handle(ButtonInteractionEvent event) {
        log.info("Refresh button handle method");
        String[] split = event.getCustomId().split(",");
        String snowflake = split[0];
        String location = split[1];
        String unit = split[2];
        String cacheKey = snowflake + "," + location + "," + unit; // TODO: fix this
        log.info("cacheKey - {}", cacheKey);
        WeatherData data = null;
        // try {
        weatherService.evictOpenWeatherCache(cacheKey);
        // log.info(
        //         "button clicked. interaction event embed details: {}",
        //         event.getMessage().get().getEmbeds().get(0).getDescription().get().split(cacheKey));
        String descriptionFromEmbed =
                event.getMessage().get().getEmbeds().get(0).getDescription().get();

        double[] coords = extractCoordinates(descriptionFromEmbed);
        String lat = String.valueOf(coords[0]);
        String lng = String.valueOf(coords[1]);
        // data = weatherService.getOpenWeatherFromCoordinates(geoService.getGeoResults(location), unit, cacheKey);
        // } catch (ApiException | InterruptedException | IOException e) {
        //     log.info("Exception with Geo Service");
        //     throw new GeoAPIExceptionHandler(e.getMessage());
        // }
        return event.edit("")
                .withEmbeds(embedResponseService.createOverviewEmbed(data, unit))
                .withComponents(embedResponseService.updateEmbedComponents(event));
        // }
    }

    private double[] extractCoordinates(String url) {
        Pattern pattern = Pattern.compile("@(-?\\d+(\\.\\d+)?),(-?\\d+(\\.\\d+)?),");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            double latitude = Double.parseDouble(matcher.group(1));
            double longitude = Double.parseDouble(matcher.group(3));
            return new double[] {latitude, longitude};
        }

        return null;
    }
}
