package com.jp.calefaction.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bot.listener.youtube")
@Getter
@Setter
public class YoutubeConfigProperties {

    private List<String> patterns;
    private boolean isDisabled;
}
