package com.jp.calefaction;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;

@SpringBootApplication
public class Calefaction {


    // fix this
    final static String DISCORD_API_KEY = "DiscordToken";
    private final static String key = System.getenv(DISCORD_API_KEY);

    public static void main(String[] args) {
        //Start spring application
        new SpringApplicationBuilder(Calefaction.class)
            .build()
            .run(args);
    }


    @Bean
    GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(key).build()
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
