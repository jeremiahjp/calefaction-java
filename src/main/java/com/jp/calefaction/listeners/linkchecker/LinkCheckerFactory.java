package com.jp.calefaction.listeners.linkchecker;

import com.jp.calefaction.config.LinkCheckerProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LinkCheckerFactory {

    private final LinkCheckerProperties properties;

    public LinkCheckerFactory(LinkCheckerProperties properties) {
        this.properties = properties;
    }

    public List<LinkChecker> createLinkCheckers() {
        List<LinkChecker> checkers = new ArrayList<>();
        properties.getLinkCheckers().forEach((type, config) -> {
            switch (type.toLowerCase()) {
                case "youtube" -> checkers.add(new YoutubeLinkChecker(config.getPatterns(), config.isDisabled()));
                case "twitter" -> checkers.add(new TwitterLinkChecker(config.getPatterns(), config.isDisabled()));
                case "instagram" -> checkers.add(new InstagramLinkChecker(config.getPatterns(), config.isDisabled()));
                case "reddit" -> checkers.add(new RedditLinkFixer(config.getPatterns(), config.isDisabled()));
                    // default -> throw new IllegalArgumentException("Unknown link checker type: " + type);
            }
        });
        return checkers;
    }
}
