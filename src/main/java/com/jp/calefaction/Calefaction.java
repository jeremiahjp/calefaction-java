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
    final String DISCORD_API_KEY = "discordAPIKey";
    String key = System.getProperty(DISCORD_API_KEY);

    public static void main(String[] args) {
        //Start spring application
        new SpringApplicationBuilder(Calefaction.class)
            .build()
            .run(args);
    }


    @Bean
    GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create("MjY4MTcxNzQwODU1NjY0NjQx.WHQoZQ.z6msaS2majd2GotwN8b7QY2p9GI").build()
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
