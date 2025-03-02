package com.jp.calefaction.listeners;

import com.jp.calefaction.config.LinkCheckerProperties;
import com.jp.calefaction.service.MessageTranslateService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class TwitterMessageTranslateService implements MessageTranslateService {

    private final LinkCheckerProperties linkCheckerProperties;

    public Mono<Void> processAndReply(MessageCreateEvent event, String url) {
        if (linkCheckerProperties.getLinkCheckers().get("twitter").isDisabled()) {
            log.info("X/Twitter command is disabled");
            return Mono.empty();
        }
        log.info(url);
        if (url.contains("twitter.com")) {
            log.info("twitter.com seen");
            return replyWithModifiedUrl(event, url, "twitter.com", "fxtwitter.com");
        } else if (url.contains("x.com")) {
            log.info("x.com seen");
            return replyWithModifiedUrl(event, url, "x.com", "fxtwitter.com");
        }
        log.info("Neither twitter or x");
        return Mono.empty();
    }

    public String extractUrl(String input) {
        Pattern urlPattern = Pattern.compile(
                "(https?://(?:www\\.)?(?:twitter\\.com|x\\.com)/[^\\s]+)", // look to externalize this?
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = urlPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private Mono<Void> replyWithModifiedUrl(MessageCreateEvent event, String url, String oldDomain, String newDomain) {
        String modifiedUrl = url.replace(oldDomain, newDomain);
        Message originalMessage = event.getMessage();
        return originalMessage
                .getChannel()
                .flatMap(channel -> channel.createMessage(modifiedUrl)
                        .withMessageReference(event.getMessage().getId()))
                .then();
    }
}
