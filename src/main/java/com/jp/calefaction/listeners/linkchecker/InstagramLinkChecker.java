package com.jp.calefaction.listeners.linkchecker;

import discord4j.core.object.entity.Message;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class InstagramLinkChecker extends AbstractLinkChecker {

    // Externalized regex pattern for maintainability
    private static final String TWITTER_URL_PATTERN = "(https?://(?:www\\.)?(instagram\\.com)/[^\\s]+)";

    public InstagramLinkChecker(List<String> patterns, boolean disabled) {
        super("Instagram", patterns, disabled);
    }

    @Override
    public Mono<Void> process(Message message) {
        String content = message.getContent();
        log.info("Inside the InstagramLinkChecker.process with the message: {}", content);
        // String videoId = extractVideoId(content);

        if (content.contains("ddinstagram.com")) {
            log.info("Already in correct ddinstagram.com format. Ignoring");
            return Mono.empty();
        }

        return message.getChannel()
                .flatMap(channel -> {
                    return channel.createMessage(replaceTwitterUrl(content)).withMessageReference(message.getId());
                })
                .then();
    }

    public String replaceTwitterUrl(String input) {
        // Extract the URL using the existing extractUrl method
        String url = extractUrl(input);
        if (url.isEmpty()) {
            return ""; // No URL found
        }

        // Replace 'twitter.com' or 'x.com' with 'fxtwitter.com'
        return url.replaceAll("https?://(?:www\\.)?(instagram\\.com)", "https://ddinstagram.com");
    }

    public String extractUrl(String input) {
        Pattern urlPattern = Pattern.compile(TWITTER_URL_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }
}
