package com.jp.calefaction.service.repost;

import com.jp.calefaction.listeners.linkchecker.LinkChecker;
import com.jp.calefaction.listeners.linkchecker.LinkCheckerFactory;
import discord4j.core.object.entity.Message;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LinkCheckerService {

    private final List<LinkChecker> linkCheckers;

    public LinkCheckerService(LinkCheckerFactory factory) {
        this.linkCheckers = factory.createLinkCheckers();
    }

    public Mono<Void> processMessage(Message message) {
        String content = message.getContent();
        log.info("Attempting to process the message: {}", content);

        return Mono.justOrEmpty(linkCheckers.stream()
                        .filter(checker -> {
                            log.info("is checker disabled? {}", checker);
                            return !checker.isDisabled() && checker.matches(content);
                        })
                        .findFirst())
                .flatMap(checker -> checker.process(message))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No link detected in message: {}", content);
                    return Mono.empty();
                }));
    }
}
