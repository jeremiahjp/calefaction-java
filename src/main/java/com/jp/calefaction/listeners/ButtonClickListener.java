package com.jp.calefaction.listeners;

import org.springframework.stereotype.Component;


import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ButtonClickListener {


    public ButtonClickListener(GatewayDiscordClient client) {

        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }


    public Mono<Void> handle(ButtonInteractionEvent event) {
        log.info("{} Button clicked", event.getCustomId());
        // we need to get the open weather info from cache somehow here.
        return event.deferEdit();
    }
    
}
