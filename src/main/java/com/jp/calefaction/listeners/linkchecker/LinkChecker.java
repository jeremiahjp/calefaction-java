package com.jp.calefaction.listeners.linkchecker;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface LinkChecker {
    boolean matches(String message);

    Mono<Void> process(Message message);

    boolean isDisabled();

    String getType();
}
