package com.jp.calefaction.listeners;

import com.jp.calefaction.listeners.buttons.ButtonHandler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ButtonEventListener {
    private final ApplicationContext applicationContext;

    public ButtonEventListener(GatewayDiscordClient client, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    @EventListener
    public Mono<Void> handle(ButtonInteractionEvent event) {
        log.info("Button interaction received with custom ID: {}", event.getCustomId());

        // Extract the handler name from the custom ID
        String handlerName = event.getCustomId().split("_")[0];
        log.info("Looking for handler with name: {}", handlerName);

        try {
            ButtonHandler handler = (ButtonHandler) applicationContext.getBean(handlerName);
            if (handler != null) {
                return handler.handle(event);
            }
        } catch (Exception e) {
            log.error("Error finding handler for button: {}", event.getCustomId(), e);
        }

        return handleUnknownButton(event);
    }

    private Mono<Void> handleUnknownButton(ButtonInteractionEvent event) {
        log.warn("No handler found for button with custom ID: {}", event.getCustomId());
        return event.reply("This button is not currently supported.").withEphemeral(true);
    }
}
