package com.calefaction.config;

import com.calefaction.core.CommandRegistry;
import com.calefaction.features.chat.LinkFixerService;
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
    public JDA jda(CommandRegistry commandRegistry, LinkFixerService linkFixerService) throws InterruptedException {
        if (token == null || token.isEmpty()) {
            log.error("Discord token is null or empty!");
            throw new IllegalArgumentException(
                    "Discord token must be provided in application.yml or DISCORD_TOKEN env var");
        }

        JDA jda = JDABuilder
                .createLight(token, Arrays.asList(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .addEventListeners(commandRegistry, linkFixerService)
                .setActivity(Activity.playing("/help"))
                .build();

        jda.awaitReady();
        log.info("JDA initialized and ready!");
        return jda;
    }
}
