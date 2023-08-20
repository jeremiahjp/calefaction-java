package com.jp.calefaction.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${openweather.baseUrl}")
    private String openApiBaseUrl;

    @Value("${catapi.baseUrl}")
    private String catApiBaseUrl;

    @Bean
    @Qualifier("OpenWeather")
    WebClient createWebClientForOpenWeather() {
        return WebClient.create(openApiBaseUrl);
    }

    @Bean
    @Qualifier("catapi")
    WebClient createCatWebClient() {
        return WebClient.create(catApiBaseUrl);
    }
}
