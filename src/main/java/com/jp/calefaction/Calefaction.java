package com.jp.calefaction;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Calefaction {

    // fix this
    static final String DISCORD_API_KEY = "DiscordToken";
    private static final String key = System.getenv(DISCORD_API_KEY);

    public static void main(String[] args) {
        // Start spring application
        new SpringApplicationBuilder(Calefaction.class).build().run(args);
    }

    @Bean
    GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(key)
                .build()
                .gateway()
                .setInitialPresence(ignore -> ClientPresence.online(ClientActivity.watching("ur mom")))
                .login()
                .block();
    }

    @Bean
    RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }
}
