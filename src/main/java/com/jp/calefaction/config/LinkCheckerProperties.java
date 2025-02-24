package com.jp.calefaction.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bot.listener")
@Getter
@Setter
public class LinkCheckerProperties {
    private Map<String, LinkConfig> linkCheckers;

    @Getter
    @Setter
    public static class LinkConfig {
        private List<String> patterns;
        private boolean disabled;
    }
}
