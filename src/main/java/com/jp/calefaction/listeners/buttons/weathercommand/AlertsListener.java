package com.jp.calefaction.listeners.buttons.weathercommand;

import com.jp.calefaction.listeners.buttons.ButtonHandler;
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

@Component("Alerts")
@Slf4j
@AllArgsConstructor
public class AlertsListener implements ButtonHandler {

    private final WeatherEmbedResponseService embedResponseService;
    private final CacheManager cacheManager;
    private static final String OPENWEATHER_CACHE = "openweather_cache";

    public String getCustomId(ButtonInteractionEvent event) {
        throw new UnsupportedOperationException("Unimplemented method 'getCustomId'");
    }

    public Mono<Void> handle(ButtonInteractionEvent event) {
        log.info("{} handle called", this.getClass().getSimpleName());

        String[] split = event.getCustomId().split(",");

        String snowflake = split[0];
        String location = split[1];
        String unit = split[2];
        String cacheKey = snowflake + "," + location + "," + unit; // TODO: fix this

        WeatherData data = getWeatherData(cacheKey);
        if (data == null) {
            log.info("Cache evicted. Disabling buttons");
            return event.edit("`Stale weather data. Submit a new command or click refresh`")
                    .withComponents(embedResponseService.disableEmbedComponents(event));
        }
        return event.edit()
                .withEmbeds(embedResponseService.createAlertsEmbed(data))
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
