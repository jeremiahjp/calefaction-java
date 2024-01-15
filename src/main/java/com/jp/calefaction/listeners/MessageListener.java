package com.jp.calefaction.listeners;

import com.jp.calefaction.service.repost.YouTubeRepostService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MessageListener {
    private final TwitterMessageTranslateService twitterMessageService;
    private final YouTubeRepostService youTubeRepostService;

    public MessageListener(
            GatewayDiscordClient gateway,
            TwitterMessageTranslateService twitterMessageService,
            YouTubeRepostService youTubeRepostService) {
        this.twitterMessageService = twitterMessageService;
        this.youTubeRepostService = youTubeRepostService;
        gateway.on(MessageCreateEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(MessageCreateEvent event) {
        if (event.getMessage().getAuthor().get().getId().asLong() != 94220323628523520L) {
            return Mono.empty();
        }
        if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) {
            log.info("Message is from a bot, ignoring.");
            return Mono.empty();
        } else {

            // Extract the content of the message
            String messageContent = event.getMessage().getContent();

            // Check for specific URLs in the message
            if (messageContent.contains("twitter.com") || messageContent.contains("x.com")) {
                log.info("Saw message with twitter.com or x.com");
                return twitterMessageService.processAndReply(event, twitterMessageService.extractUrl(messageContent));
            }
            if (messageContent.contains("youtube.com/watch?v") || messageContent.contains("youtu.be/")) {
                return youTubeRepostService.handle(event);
            }
            log.info("This message has no action to take");
            return Mono.empty();
        }
    }
}
