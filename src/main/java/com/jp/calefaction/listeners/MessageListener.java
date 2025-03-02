package com.jp.calefaction.listeners;

import com.jp.calefaction.service.repost.LinkCheckerService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class MessageListener {
    private final GatewayDiscordClient gateway;
    // private final TwitterMessageTranslateService twitterMessageService;
    // private final YoutubeMessageListener youtubeMessageListener;
    // private final ChatGptModerationService chatGptModerationService;
    // private final DynamicLinkChecker dynamicLinkChecker;
    private final LinkCheckerService linkCheckerService;

    // @Value("${chatGPT.moderation.enabled}")
    // private boolean moderationEnabled;

    // @Value("${bot.listener.youtube.disabled}")
    // private boolean youtubeReposterDisabled;

    @PostConstruct
    public void init() {
        gateway.on(MessageCreateEvent.class, this::handle).subscribe();
    }

    /**
     * On Every MessageCreateEvent, this handle method can be considered the entrypoint to the
     * bot on any message sent in any channel.
     * In other words, all created messages flow through here.
     * @param event
     * @return
     */
    public Mono<Void> handle(MessageCreateEvent event) {
        if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) {
            log.info("Message is from a bot, ignoring.");
            return Mono.empty();
        }

        return linkCheckerService.processMessage(event.getMessage());

        // if (!moderationEnabled) {
        //     return checkForURLsAndAct(event, messageContent); //TODO: implement moderation
        // }

        //  Mono.justOrEmpty(linkCheckerService.findLinkType(messageContent))
        //  .flatMap(linkType -> {
        //     dynamicLinkChecker.
        //  })

        // return chatGptModerationService.process(messageContent)
        //     .flatMap(flaggedCategories -> {
        //         if (!flaggedCategories.isEmpty()) {
        //             // Prepare a response message
        //             StringJoiner joiner = new StringJoiner(", ");
        //             flaggedCategories.forEach(joiner::add);
        //             String responseMessage = "`whoa whoa whoa that was " + joiner.toString() + "`";

        //             // Reply to the chat message with the flagged categories
        //             Mono<Void> replyMono = event.getMessage()
        //                     .getChannel()
        //                     .flatMap(channel -> channel.createMessage(responseMessage)
        //                             .withMessageReference(event.getMessage().getId())
        //                             .withContent(responseMessage))
        //                     .then();

        //             // Continue processing after replying
        //             return replyMono.then(delegateMessage(event, messageContent));
        //         } else {
        //             // If there are no flagged categories, continue processing
        //             return delegateMessage(event, messageContent);
        //         }
        //     });
    }

    // private Mono<Void> delegateMessage(MessageCreateEvent event) {
    //     // Check for specific URLs in the message
    //     String message = event.getMessage().getContent();

    //     if (youtubeMessageListener.containsMatchedString(message)) {
    //         return youtubeMessageListener.handle(event);
    //     }

    //     if (message.contains("twitter.com") || message.contains("x.com")) {
    //         log.info("Saw message with twitter.com or x.com");
    //         return twitterMessageService.processAndReply(event, twitterMessageService.extractUrl(messageContent));
    //     }
    //     // if (message.contains("youtube.com/watch?v") // TODO: Fix this dupe issue
    //     //         || message.contains("youtu.be/")
    //     //         || message.contains("youtube.com/shorts/")) {
    //     //     return youtubeMessageListener.handle(event);
    //     // }
    //     log.info("This message has no action to take.\nThe message was: {}", messageContent);
    //     return Mono.empty();
    // }
}
