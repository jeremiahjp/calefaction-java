package com.jp.calefaction.service.repost;

import com.jp.calefaction.entity.OriginalMessages;
import com.jp.calefaction.entity.Users;
import com.jp.calefaction.entity.repost.RepostCount;
import com.jp.calefaction.service.RepostEmbedService;
import com.jp.calefaction.service.RepostService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class YouTubeRepostService {

    private final OriginalMessagesService originalMessagesService;

    private final RepostService repostService;

    private final UsersService usersService;

    private final RepostEmbedService repostEmbedService;

    private final RepostCountService repostCountService;

    private final List<String> keywords = new ArrayList<>();

    String[] ytMatch = {"www.youtube.com/watch?v", "youtu.be/"};

    public YouTubeRepostService(
            OriginalMessagesService originalMessagesService,
            GatewayDiscordClient gateway,
            RepostService repostService,
            UsersService usersService,
            RepostEmbedService repostEmbedService,
            RepostCountService repostCountService) {
        this.repostEmbedService = repostEmbedService;
        this.usersService = usersService;
        this.originalMessagesService = originalMessagesService;
        this.repostService = repostService;
        this.repostCountService = repostCountService;
        keywords.add("YouTube");
        // gateway.on(MessageCreateEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(MessageCreateEvent event) {
        // Check if the message is from a bot
        if (event.getMessage().getAuthor().map(User::isBot).orElse(false)) {
            log.info("Message is from a bot, ignoring.");
            return Mono.empty();
        }
        return Mono.just(event).flatMap(e -> processMessage(e.getMessage())).then();
    }

    private Mono<Void> processMessage(Message message) {
        log.info("User is a bot? {}", message.getAuthor().get().isBot());
        String messageContent = message.getContent();
        // Example of processing for YouTube URLs
        log.info("Checking if it contains youtube");
        if (messageContent.contains("youtube.com/watch?v") // TODO: Fix this
                || messageContent.contains("youtu.be/")
                || messageContent.contains("youtube.com/shorts/")) {
            log.info("Message contains youtube or youtub.be or shorts. Lets try and process it");
            return processYouTubeLink(messageContent, message);
        }
        log.info("Didn't detect youtube at all, ignoring");
        // Add other URL processing logic as needed
        return Mono.empty();
    }

    private Mono<Void> processYouTubeLink(String messageContent, Message message) {
        log.info("messageContent is {}", messageContent);
        String videoId = repostService.extractVideoIdSync(messageContent); // need to write this as async
        // no further processing because no videoId
        if (videoId == null) {
            return Mono.empty();
        }

        return originalMessagesService
                // find in the database
                .findByUrlKeyAndGuildId(
                        videoId, message.getGuildId().orElse(null).asString())
                .flatMap(originalMessage -> {
                    log.info("inside .flatMap in processYouTubeLink");
                    // If exists in Original_Messages, then insert into Repost_Count and return an embed
                    return saveToRepostCount(message, originalMessage)
                            .then(sendRepostEmbedMessage(message, originalMessage))
                            .thenReturn(originalMessage);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // If it does not exist, then insert into Users and Original_Messages
                    log.info("inside .switchIfEmpty in processYouTubeLink");
                    return createAndSaveEntities(videoId, message);
                }))
                .then();
    }

    // this is called IF original_messaged doesnt return an entry
    // We just want to save to Users and Original Messages
    private Mono<OriginalMessages> createAndSaveEntities(String videoId, Message message) {
        log.info("Create and save entities with videoId and message content: {}, {}", videoId, message.getContent());
        String userId = message.getAuthor()
                .map(User::getId)
                .map(snowflake -> snowflake.asString())
                .orElse(null);
        String guildId =
                message.getGuildId().map(snowflake -> snowflake.asString()).orElse(null);
        log.info("Attempting to find by user id as {}", userId);

        // save to users repo if no user exists
        Mono<Users> userMono = usersService
                .findById(userId)
                .doOnNext(user -> log.info("User found with ID: {}", user.getSnowflakeId()))
                .switchIfEmpty(Mono.defer(() -> {
                    Users newUser = new Users();
                    newUser.setSnowflakeId(userId);
                    newUser.setCreatedOn(Instant.now());
                    log.info("Creating new user with ID: {}", userId);
                    return usersService.save(newUser);
                }))
                .doOnSuccess(user -> log.info("User saved with ID: {}", user.getSnowflakeId()));

        // then save to original messages
        return userMono.then(Mono.defer(() -> {
            OriginalMessages newOriginalMessage = new OriginalMessages();
            newOriginalMessage.setUrlKey(videoId);
            newOriginalMessage.setGuildId(guildId);
            newOriginalMessage.setChannelId(message.getChannelId().asString());
            newOriginalMessage.setMessageId(message.getId().asString());
            newOriginalMessage.setCapturedUrl(extractUrl(message.getContent()));
            newOriginalMessage.setOriginalUrl(extractUrl(message.getContent()));
            newOriginalMessage.setCreatedOn(Instant.now());
            newOriginalMessage.setSnowflakeId(userId);
            newOriginalMessage.setUrlDomain("youtube"); // todo: fix me to actually store the domain?
            log.info("Attempting to save the message: {}", newOriginalMessage);
            return originalMessagesService.save(newOriginalMessage);
        }));
    }

    private Mono<RepostCount> saveToRepostCount(Message message, OriginalMessages originalMessage) {
        log.info("Repost detected!");
        log.info("Original message: {}", originalMessage);

        RepostCount repost = new RepostCount();
        repost.setAttemptedRepostUrlKey(originalMessage.getUrlKey());
        repost.setCreatedOn(Instant.now());
        repost.setLastAttempted(Instant.now());
        repost.setSnowflakeId(originalMessage.getSnowflakeId());
        repost.setGuildId(message.getGuildId().get().asString());
        log.info("Saving to repost_count");
        return repostCountService.save(repost); // Returns a Mono<RepostCount>
    }

    private Mono<Void> sendRepostEmbedMessage(Message message, OriginalMessages originalMessage) {
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(repostEmbedService.createRepostEmbed(originalMessage))
                        .withMessageReference(message.getId()))
                .then();
    }

    private String extractUrl(String input) {
        String[] parts = input.split("\\s+");

        for (String part : parts) {
            if (part.contains("youtube") || part.contains("youtu.be")) {
                log.info("part found: {}", part);
                return part;
            }
        }
        return "";
    }
}
