package com.jp.calefaction.listeners.buttons.weathercommand;

import com.jp.calefaction.listeners.buttons.ButtonHandler;
import com.jp.calefaction.model.weather.WeatherData;
import com.jp.calefaction.service.EmbedResponseService;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component("3-day")
@AllArgsConstructor
public class ThreeDayForecastListener implements ButtonHandler {

    private final EmbedResponseService embedResponseService;
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
        String labelOfButtonClicked = split[3];
        String cacheKey = snowflake + "," + location + "," + unit; // TODO: fix this

        WeatherData data = getWeatherData(cacheKey);
        if (data == null) {
            log.info("Cache evicted. Disabling buttons");
            return event.edit("`Stale weather data. Submit a new command`").withComponents();
        }
        return event.edit()
                .withEmbeds(embedResponseService.createThreeDay(data, unit))
                .withComponents(updateButtons(event, labelOfButtonClicked));
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

    private List<LayoutComponent> updateButtons(ButtonInteractionEvent event, String clickedButtonId) {
        log.info("clicked button id {}, actual button id {}", clickedButtonId, event.getCustomId());
        // TODO: Fix, this code sucks and was just POC
        List<LayoutComponent> actions = new ArrayList<>();
        List<LayoutComponent> components = event.getMessage().get().getComponents();

        List<ActionRow> actionRows = components.stream()
                .filter(component -> component instanceof ActionRow)
                .map(component -> (ActionRow) component)
                .collect(Collectors.toList());

        List<Button> buttons = actionRows.stream()
                .flatMap(actionRow -> actionRow.getChildren().stream())
                .filter(item -> item instanceof Button)
                .map(item -> (Button) item)
                .collect(Collectors.toList());

        List<Button> newButtons = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i++) {
            Button tempButton;
            if (buttons.get(i).getCustomId().get().equals(event.getCustomId())) {
                log.info("same button clicked");
                tempButton = buttons.get(i).disabled(true);
            } else tempButton = buttons.get(i).disabled(false);
            newButtons.add(tempButton);
        }
        ActionRow row1 = ActionRow.of(newButtons.get(0), newButtons.get(1), newButtons.get(2));
        ActionRow row2 = ActionRow.of(newButtons.get(3), newButtons.get(4), newButtons.get(5));
        actions.add(row1);
        actions.add(row2);
        return actions;
    }
}
