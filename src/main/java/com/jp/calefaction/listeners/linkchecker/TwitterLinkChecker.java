package com.jp.calefaction.listeners.linkchecker;

import discord4j.core.object.entity.Message;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class TwitterLinkChecker extends AbstractLinkChecker {

    // Externalized regex pattern for maintainability
    private static final String TWITTER_URL_PATTERN = "(https?://(?:www\\.)?(twitter\\.com|x\\.com)/[^\\s]+)";

    public TwitterLinkChecker(List<String> patterns, boolean disabled) {
        super("Twitter", patterns, disabled);
    }

    @Override
    public Mono<Void> process(Message message) {
        String content = message.getContent();
        log.info("Inside the TwitterLinkChecker.process with the message: {}", content);
        // String videoId = extractVideoId(content);

        if (content.contains("fxtwitter.com")) {
            log.info("Already in correct fxtwitter.com format. Ignoring");
            return Mono.empty();
        }

        return message.getChannel()
                .flatMap(channel -> {
                    return channel.createMessage(replaceTwitterUrl(content)).withMessageReference(message.getId());
                })
                .then();
    }

    public String extractUrl(String input) {
        Pattern urlPattern = Pattern.compile(TWITTER_URL_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    public String replaceTwitterUrl(String input) {
        // Extract the URL using the existing extractUrl method
        String url = extractUrl(input);
        if (url.isEmpty()) {
            return ""; // No URL found
        }

        // Replace 'twitter.com' or 'x.com' with 'fxtwitter.com'
        return url.replaceAll("https?://(?:www\\.)?(twitter\\.com|x\\.com)", "https://fxtwitter.com");
    }
}
