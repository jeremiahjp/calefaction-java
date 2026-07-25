package com.calefaction.features.chat;

import com.calefaction.config.LinkFixerConfig;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LinkFixerService extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(LinkFixerService.class);
    private final LinkFixerConfig config;

    // Matches any http/https URL with at least one dot in the domain
    // Group 1: protocol (https://)
    // Group 2: www prefix (optional)
    // Group 3: domain (e.g. twitter.com)
    // Group 4: path (/...)
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://)(www\\.)?([a-zA-Z0-9.-]+\\.[a-z]{2,})(/\\S*)",
            Pattern.CASE_INSENSITIVE);

    public LinkFixerService(LinkFixerConfig config) {
        this.config = config;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!config.isEnabled())
            return;
        if (event.getAuthor().isBot() || event.isWebhookMessage()
                || event.getAuthor().equals(event.getJDA().getSelfUser()))
            return;

        String content = event.getMessage().getContentRaw();
        Matcher matcher = URL_PATTERN.matcher(content);

        if (matcher.find()) {
            StringBuilder fixedContent = new StringBuilder();
            boolean found = false;

            matcher.reset();
            while (matcher.find()) {
                String domain = matcher.group(3).toLowerCase();
                String path = matcher.group(4);

                // Find matching config for this domain
                String replacement = null;
                for (LinkFixerConfig.DomainConfig dc : config.getDomains()) {
                    if (dc.isEnabled() && domain.equals(dc.getPattern())) {
                        replacement = dc.getReplacement();
                        break;
                    }
                }

                if (replacement != null) {
                    // Reconstruct URL: https:// + replacement + path
                    String fixedUrl = "https://" + replacement + path;
                    fixedContent.append(fixedUrl).append("\n");
                    found = true;
                }
            }

            if (found) {
                try {
                    event.getMessage().suppressEmbeds(true).queue(
                            success -> log.debug("Suppressed original embeds for message {}", event.getMessageId()),
                            error -> log.debug("Could not suppress embeds (likely missing permission)"));
                } catch (Exception e) {
                    // Ignore
                }

                event.getMessage().reply(fixedContent.toString().trim())
                        .setAllowedMentions(java.util.Collections.emptyList())
                        .queue();

                log.info("Fixed links for user {}: {}", event.getAuthor().getName(), fixedContent);
            }
        }
    }
}
