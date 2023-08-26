package com.jp.calefaction.listeners.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface ButtonHandler {

    String getCustomId(ButtonInteractionEvent event);

    Mono<Void> handle(ButtonInteractionEvent event);
}
