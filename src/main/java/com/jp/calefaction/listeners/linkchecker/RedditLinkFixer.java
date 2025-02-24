package com.jp.calefaction.listeners.linkchecker;

import discord4j.core.object.entity.Message;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class RedditLinkFixer extends AbstractLinkChecker {

    // Externalized regex pattern for maintainability
    private static final String REDDIT_URL_PATTERN = "(https?://(?:www\\.)?(reddit\\.com)/[^\\s]+)";

    public RedditLinkFixer(List<String> patterns, boolean disabled) {
        super("Reddit", patterns, disabled);
    }

    @Override
    public Mono<Void> process(Message message) {
        String content = message.getContent();
        log.info("Inside the RedditLinkFixer.process with the message: {}", content);
        // String videoId = extractVideoId(content);

        if (content.contains("rxddit.com")) {
            log.info("Already in correct rxddit.com format. Ignoring");
            return Mono.empty();
        }

        return message.getChannel()
                .flatMap(channel -> {
                    return channel.createMessage(redditReplaceUrl(content)).withMessageReference(message.getId());
                })
                .then();
    }

    public String extractUrl(String input) {
        Pattern urlPattern = Pattern.compile(REDDIT_URL_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    public String redditReplaceUrl(String input) {
        // Extract the URL using the existing extractUrl method
        String url = extractUrl(input);
        if (url.isEmpty()) {
            return ""; // No URL found
        }

        // Replace 'reddit.com' with 'rxddit.com'
        return url.replaceAll("https?://(?:www\\.)?(reddit\\.com)", "https://rxddit.com");
    }
}
