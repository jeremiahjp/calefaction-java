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

    private final List<String> keywords = new ArrayList<>();

    String[] ytMatch = {"www.youtube.com/watch?v", "youtu.be/"};

    public MessageListener(
            OriginalMessagesRepository originalMessagesRepository,
            GatewayDiscordClient gateway,
            RepostService repostService,
            UsersRepository usersRepository,
            RepostEmbedService repostEmbedService) {
        this.repostEmbedService = repostEmbedService;
        this.usersRepository = usersRepository;
        this.originalMessagesRepository = originalMessagesRepository;
        this.repostService = repostService;
        keywords.add("YouTube");
        gateway.on(MessageCreateEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(MessageCreateEvent event) {
        // if (event.getMessage().getAuthor().
        // MessageCreateEvent example
        // Mono<Void> handlePingCommand = gateway.on(MessageCreateEvent.class, event1 -> {
        Message message = event.getMessage();
        event.getMessage().getUserData();
        log.info(message.getContent());
        log.info(keywords.get(0));
        String s = parseString(message.getContent());
        String s1 = extractUrl(message.getContent());
        // if (keywords.contains(message.getContent())) {

        String[] tokens = message.getContent().split(" ");

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
                // /** this code is good
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
                log.info("it you");
                event.getMessage()
                        .getChannel()
                        .block()
                        .createMessage(repostEmbedService.createRepostEmbed(originalMessages))
                        .block();
                // }
            }

            // */
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
