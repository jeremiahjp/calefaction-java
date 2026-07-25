package com.calefaction.features.chat;

import com.calefaction.config.LinkFixerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LinkFixerService extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(LinkFixerService.class);
    private final LinkFixerConfig config;
    private final VideoDownloadService videoDownloadService;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://)(www\\.)?([a-zA-Z0-9.-]+\\.[a-z]{2,})(/\\S*)",
            Pattern.CASE_INSENSITIVE);

    public LinkFixerService(LinkFixerConfig config, VideoDownloadService videoDownloadService) {
        this.config = config;
        this.videoDownloadService = videoDownloadService;
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
            List<String> originalUrls = new ArrayList<>();
            List<String> fixedUrls = new ArrayList<>();
            boolean found = false;

            matcher.reset();
            while (matcher.find()) {
                String fullUrl = matcher.group(0);
                String domain = matcher.group(3).toLowerCase();
                String path = matcher.group(4);

                String replacement = null;
                for (LinkFixerConfig.DomainConfig dc : config.getDomains()) {
                    if (dc.isEnabled() && domain.equals(dc.getPattern())) {
                        replacement = dc.getReplacement();
                        break;
                    }
                }

                if (replacement != null) {
                    originalUrls.add(fullUrl);
                    String fixedUrl = "https://" + replacement + path;
                    fixedUrls.add(fixedUrl);
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

                if ("DOWNLOAD".equalsIgnoreCase(config.getMode())) {
                    for (String url : originalUrls) {
                        videoDownloadService.downloadVideo(url).thenAccept(file -> {
                            event.getMessage().replyFiles(FileUpload.fromData(file))
                                    .setAllowedMentions(java.util.Collections.emptyList())
                                    .queue(
                                        success -> file.delete(),
                                        error -> {
                                            log.error("Failed to upload downloaded video", error);
                                            file.delete();
                                        }
                                    );
                        }).exceptionally(ex -> {
                            log.error("Failed to download video from {}", url, ex);
                            event.getMessage().reply("❌ Sorry, I couldn't download the video from that link. It might be too large (Discord limits bots to 25MB) or the platform is blocking downloads.")
                                    .setAllowedMentions(java.util.Collections.emptyList())
                                    .queue();
                            return null;
                        });
                    }
                } else {
                    StringBuilder fixedContent = new StringBuilder();
                    for (String url : fixedUrls) {
                        fixedContent.append(url).append("\n");
                    }
                    event.getMessage().reply(fixedContent.toString().trim())
                            .setAllowedMentions(java.util.Collections.emptyList())
                            .queue();
                    log.info("Fixed links for user {}: {}", event.getAuthor().getName(), fixedContent);
                }
            }
        }
    }
}
