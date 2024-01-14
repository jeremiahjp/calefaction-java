package com.jp.calefaction.listeners.buttons.weathercommand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.calefaction.listeners.buttons.ButtonHandler;
import com.jp.calefaction.model.weather.ButtonData;
import com.jp.calefaction.model.weather.WeatherData;
import com.jp.calefaction.service.WeatherEmbedResponseService;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("Astronomy")
@Slf4j
@AllArgsConstructor
public class AstronomyListener implements ButtonHandler {

    private final WeatherEmbedResponseService embedResponseService;
    private final CacheManager cacheManager;
    private static final String OPENWEATHER_CACHE = "openweather_cache";
    private final ObjectMapper jsonMapper;

    public String getCustomId(ButtonInteractionEvent event) {
        throw new UnsupportedOperationException("Unimplemented method 'getCustomId'");
    }

    public Mono<Void> handle(ButtonInteractionEvent event) {
        log.info("{} handle called", this.getClass().getSimpleName());
        log.info("Button clicked with customId: {}", event.getCustomId());

        ButtonData buttonData;
        try {
            buttonData = jsonMapper.readValue(event.getCustomId(), ButtonData.class);
        } catch (JsonProcessingException e) {
            log.error("There was an error mapping to buttonData");
            e.printStackTrace();
            return event.reply("There was an error processing this request.").withEphemeral(true);
        }
        WeatherData data = getWeatherData(buttonData.getCacheId());

        if (data == null) {
            log.info("Not found in cache. Disabling buttons");
            return event.edit("`Stale weather data. Submit a new command or click refresh`")
                    .withComponents(embedResponseService.disableEmbedComponents(event));
        }
        return event.edit()
                .withEmbeds(embedResponseService.createAstronomyEmbed(data))
                .withComponents(embedResponseService.updateEmbedComponents(event));
    }

    public WeatherData getWeatherData(String key) {
        Cache cache = cacheManager.getCache(OPENWEATHER_CACHE);
        if (cache != null) {
            ValueWrapper valueWrapper = cache.get(key);
            if (valueWrapper != null) {
                log.info("exists in cache");
                return (WeatherData) valueWrapper.get();
            }
        }
        return null;
    }
}
