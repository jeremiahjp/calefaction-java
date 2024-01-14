package com.jp.calefaction.listeners;

import com.jp.calefaction.entity.OriginalMessages;
import com.jp.calefaction.entity.Users;
import com.jp.calefaction.repository.OriginalMessagesRepository;
import com.jp.calefaction.repository.UsersRepository;
import com.jp.calefaction.service.RepostEmbedService;
import com.jp.calefaction.service.RepostService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MessageListener {

    private final OriginalMessagesRepository originalMessagesRepository;

    private final RepostService repostService;

    private final UsersRepository usersRepository;

    private final RepostEmbedService repostEmbedService;

    private final UrlReplyService urlReplyService;

    private final List<String> keywords = new ArrayList<>();

    String[] ytMatch = {"www.youtube.com/watch?v", "youtu.be/"};

    public MessageListener(
            OriginalMessagesRepository originalMessagesRepository,
            GatewayDiscordClient gateway,
            RepostService repostService,
            UsersRepository usersRepository,
            RepostEmbedService repostEmbedService,
            UrlReplyService urlReplyService) {
        this.repostEmbedService = repostEmbedService;
        this.usersRepository = usersRepository;
        this.originalMessagesRepository = originalMessagesRepository;
        this.repostService = repostService;
        this.urlReplyService = urlReplyService;
        keywords.add("YouTube");
        gateway.on(MessageCreateEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(MessageCreateEvent event) {

        Optional<User> author = event.getMessage().getAuthor();
        if (author.isPresent() && author.get().isBot()) {
            return Mono.empty();
        }
        // if (event.getMessage().getAuthor().
        // MessageCreateEvent example
        // Mono<Void> handlePingCommand = gateway.on(MessageCreateEvent.class, event1 -> {
        Message message = event.getMessage();
        String messageContent = message.getContent();
        // Check for Twitter URLs
        if (messageContent.contains("twitter.com") || messageContent.contains("x.com")) {
            log.info("Saw message with twitter.com or x.com");
            return urlReplyService.processAndReply(event, urlReplyService.extractUrl(messageContent));
        }

        event.getMessage().getUserData();
        log.debug(message.getContent());
        log.debug(keywords.get(0));
        String s = parseString(message.getContent());
        String s1 = extractUrl(message.getContent());
        // if (keywords.contains(message.getContent())) {
        String key = repostService.extractVideoId(s1);

        if (key != null) {
            Optional<Users> u =
                    usersRepository.findById(event.getMember().get().getId().asString());
            if (u.isEmpty()) {
                log.info("new user {}", event.getMember().get().getId().asString());
                Users user = new Users();
                user.setSnowflakeId(event.getMember().get().getId().asString());
                user.setCreatedOn(Instant.now());
                usersRepository.save(user);
            }
            OriginalMessages originalMessages = repostService.getByIdAndGuild(
                    key, message.getGuildId().get().asString());
            if (originalMessages == null) {
                log.info("contained a keyword\nLogging it..");
                // String s = parseString(message.getContent());
                OriginalMessages data = new OriginalMessages();
                data.setChannelId(message.getChannelId().asString());
                data.setCreatedOn(Instant.now());
                data.setGuildId(message.getGuildId().get().asString());
                data.setMessageId(message.getId().asString());
                data.setSnowflakeId(message.getAuthor().get().getId().asString());
                data.setCapturedUrl(extractUrl(s));
                data.setOriginalUrl(s1);
                data.setUrlKey(key);
                data.setUrlDomain("fixme");

                originalMessagesRepository.save(data);
            } else {
                log.info("REPOSTER!\nThis was posted by: @{} ", u.get().getSnowflakeId());
                // if (event.getMember().get().getId().asString().equals("94220323628523520")) {
                event.getMessage()
                        .getChannel()
                        .block()
                        .createMessage(repostEmbedService.createRepostEmbed(originalMessages))
                        .block();
                // }
            }

            // TODO: store to database for tracking of reposts
            // this is where logic for saving to database will exist
        }

        return Mono.empty();
    }

    private String parseString(String input) {
        // Define the patterns for substrings
        Pattern pattern = Pattern.compile("youtube|vimeo|steam", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        // Replace matched substrings with an empty string
        String parsedString = matcher.replaceAll("");

        return parsedString;
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
