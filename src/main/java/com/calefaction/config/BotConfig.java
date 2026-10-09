package com.calefaction.config;

import java.util.Arrays;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfig {

    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    @Value("${discord.token}")
    private String token;

    @Bean
    public JDA jda(java.util.List<net.dv8tion.jda.api.hooks.ListenerAdapter> listeners) throws InterruptedException {
        if (token == null || token.isEmpty()) {
            log.error("Discord token is null or empty!");
            throw new IllegalArgumentException(
                    "Discord token must be provided in application.yml or DISCORD_TOKEN env var");
        }

        var builder = JDABuilder
                .createLight(token, Arrays.asList(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .setActivity(Activity.playing("/help"));

        for (var listener : listeners) {
            builder.addEventListeners(listener);
            log.info("Registered JDA listener: {}", listener.getClass().getSimpleName());
        }

        JDA jda = builder.build();
        jda.awaitReady();
        log.info("JDA initialized and ready with {} listeners!", listeners.size());
        return jda;
    }
}
