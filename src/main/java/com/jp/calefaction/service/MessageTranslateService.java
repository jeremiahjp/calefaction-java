package com.jp.calefaction.service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public interface MessageTranslateService {
    Mono<Void> processAndReply(MessageCreateEvent event, String url);

    String extractUrl(String input);
}
