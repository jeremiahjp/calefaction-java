package com.jp.calefaction.listeners.linkchecker;

import discord4j.core.object.entity.Message;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class YoutubeLinkChecker extends AbstractLinkChecker {

    public YoutubeLinkChecker(List<String> patterns, boolean disabled) {
        super("YouTube", patterns, disabled);
    }

    @Override
    public Mono<Void> process(Message message) {
        log.info("Inside the YoutubeLinkChecker.process with the message: {}", message.getContent());
        String content = message.getContent();
        String videoId = extractVideoId(content);

        if (videoId != null) {
            log.info("FOUND A LINK, WOOOO HOO. Youtube id: {}", videoId);
            // in the future could insert this into a database.
            return Mono.empty();
            // return message.getChannel()
            //         .flatMap(channel -> channel.createMessage("Detected a YouTube video! Video ID: " + videoId))
            //         .then();
        }
        return Mono.empty();
    }

    private String extractVideoId(String message) {
        for (String pattern : getPatterns()) {
            String regex =
                    switch (pattern) {
                        case "youtube.com/watch?v" -> "youtube\\.com/watch\\?v=([^&\\s]+)";
                        case "youtu.be/" -> "youtu\\.be/([^&\\s]+)";
                        case "youtube.com/shorts/" -> "youtube\\.com/shorts/([^&\\s]+)";
                        default -> null;
                    };

            if (regex != null) {
                Pattern compiledPattern = Pattern.compile(regex);
                Matcher matcher = compiledPattern.matcher(message);
                if (matcher.find()) {
                    return matcher.group(1); // Group 1 contains the videoId
                }
            }
        }
        return null;
    }
}
