package com.jp.calefaction.listeners;

import com.jp.calefaction.service.repost.YouTubeRepostService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MessageListener {
    private final TwitterMessageTranslateService twitterMessageService;
    private final YouTubeRepostService youTubeRepostService;
    private final ChatGptModerationService chatGptModerationService;

    @Value("${chatGPT.moderation.enabled}")
    private boolean moderationEnabled;

    public MessageListener(
            GatewayDiscordClient gateway,
            TwitterMessageTranslateService twitterMessageService,
            YouTubeRepostService youTubeRepostService,
            ChatGptModerationService chatGptModerationService) {
        this.twitterMessageService = twitterMessageService;
        this.youTubeRepostService = youTubeRepostService;
        this.chatGptModerationService = chatGptModerationService;
        gateway.on(MessageCreateEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(MessageCreateEvent event) {
        // if (event.getMessage().getGuildId().get().asLong() == 107536202030129152L) {
        //     return Mono.empty();
        // }
        if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) {
            log.info("Message is from a bot, ignoring.");
            return Mono.empty();
        } else {
            // Extract the content of the message
            String messageContent = event.getMessage().getContent();
            if (!moderationEnabled) {
                return checkForURLsAndAct(event, messageContent);
            }

            return chatGptModerationService.process(messageContent).flatMap(flaggedCategories -> {
                if (!flaggedCategories.isEmpty()) {
                    // Prepare a response message
                    StringJoiner joiner = new StringJoiner(", ");
                    flaggedCategories.forEach(joiner::add);
                    String responseMessage = "`whoa whoa whoa that was " + joiner.toString() + "`";

                    // Reply to the chat message with the flagged categories
                    Mono<Void> replyMono = event.getMessage()
                            .getChannel()
                            .flatMap(channel -> channel.createMessage(responseMessage)
                                    .withMessageReference(event.getMessage().getId())
                                    .withContent(responseMessage))
                            .then();

                    // Continue processing after replying
                    return replyMono.then(checkForURLsAndAct(event, messageContent));
                } else {
                    // If there are no flagged categories, continue processing
                    return checkForURLsAndAct(event, messageContent);
                }
            });
        }
    }

    private Mono<Void> checkForURLsAndAct(MessageCreateEvent event, String messageContent) {
        // Check for specific URLs in the message
        if (messageContent.contains("twitter.com") || messageContent.contains("x.com")) {
            log.info("Saw message with twitter.com or x.com");
            return twitterMessageService.processAndReply(event, twitterMessageService.extractUrl(messageContent));
        }
        if (messageContent.contains("youtube.com/watch?v") // TODO: Fix this dupe issue
                || messageContent.contains("youtu.be/")
                || messageContent.contains("youtube.com/shorts/")) {
            return youTubeRepostService.handle(event);
        }
        log.info("This message has no action to take.\nThe message was: {}", messageContent);
        return Mono.empty();
    }
}
