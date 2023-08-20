package com.jp.calefaction.listeners;

import com.jp.calefaction.model.weather.WeatherData;
import com.jp.calefaction.service.WeatherService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ButtonClickListener {

    private final CacheManager cacheManager;
    private final WeatherService weatherService;
    private static final String OPENWEATHER_CACHE = "openweather_cache";

    public ButtonClickListener(GatewayDiscordClient client, CacheManager cacheManager, WeatherService weatherService) {
        this.weatherService = weatherService;
        this.cacheManager = cacheManager;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(ButtonInteractionEvent event) {

        log.info("customId of button clicked: {}", event.getCustomId());

        String[] split = event.getCustomId().split(",");

        String snowflake = split[0];
        String location = split[1];
        String unit = split[2];
        String labelOfButtonClicked = split[3];

        long idOfUserThatClicked = event.getInteraction().getUser().getId().asLong();

        if (!event.getCustomId().contains(String.valueOf(idOfUserThatClicked))) {
            log.info("this person attempted to click {}", idOfUserThatClicked);
            return event.reply("You are not authorized to do that").withEphemeral(true);
        }
        log.info("proper discord id {}", snowflake);

        String cacheKey = snowflake + "," + location + "," + unit; // TODO: fix this
        return handleButtonInteractions(labelOfButtonClicked, cacheKey, event);
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

    public Mono<Void> handleButtonInteractions(String labelOfButtonClicked, String key, ButtonInteractionEvent event) {
        WeatherData data = getWeatherData(key);
        if (data == null) {
            log.info("Cache evicted. Disabling buttons");
            return event.edit("`Stale weather data. Submit a new command`")
                .withComponents();
        }
        switch (labelOfButtonClicked.toLowerCase()) {
            case "3-day":
                return event.edit()
                        .withEmbeds(weatherService.createThreeDay(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "5-day":
                return event.edit()
                        .withEmbeds(weatherService.createFiveDayEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "current":
                return event.edit()
                        .withEmbeds(weatherService.createCurrentEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "overview":
                return event.edit()
                        .withEmbeds(weatherService.createCurrentEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "astronomy":
                return event.edit()
                        .withEmbeds(weatherService.createAstronomyEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "airquality":
                return event.edit()
                        .withEmbeds(weatherService.createAirQualityEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "forecast":
                return event.edit()
                        .withEmbeds(weatherService.createCurrentEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "showall":
                return event.edit()
                        .withEmbeds(weatherService.createVerboseEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            case "alerts":
                return event.edit()
                        .withEmbeds(weatherService.createAlertsEmbed(data))
                        .withComponents(updateButtons(event, labelOfButtonClicked));
            default:
                return event.edit("Something broke");
        }
    }

    private List<LayoutComponent> updateButtons(ButtonInteractionEvent event, String clickedButtonId) {
        log.info("clicked button id {}, actual button id {}", clickedButtonId, event.getCustomId());
        //TODO: Fix, this code sucks and was just POC
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
            } else 
                tempButton = buttons.get(i).disabled(false);
            newButtons.add(tempButton);
        }
        ActionRow row1 = ActionRow.of(newButtons.get(0), newButtons.get(1), newButtons.get(2));
        ActionRow row2 = ActionRow.of(newButtons.get(3), newButtons.get(4), newButtons.get(5));
        actions.add(row1);
        actions.add(row2);
        return actions;
    }
}
