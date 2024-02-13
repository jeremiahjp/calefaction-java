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

    @Value("${dogapi.baseUrl}")
    private String dogApiBaseUrl;

    @Value("${ercot.baseUrl}")
    private String ercotFuelMix;

    @Value("${urban_dictionary.baseUrl}")
    private String urbanBaseUrl;

    @Value("${chatGPT.baseUrl}")
    private String chatGPTUrl;

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

    @Bean
    @Qualifier("dogapi")
    WebClient createDogWebClient() {
        return WebClient.create(dogApiBaseUrl);
    }

    @Bean
    @Qualifier("ercot")
    WebClient createErcotWebClient() {
        return WebClient.create(catApiBaseUrl);
    }

    @Bean
    @Qualifier("urban")
    WebClient createUrbanDictWebClient() {
        return WebClient.create(urbanBaseUrl);
    }

    @Bean
    @Qualifier("chatgpt")
    WebClient createChatGptWebClient() {
        return WebClient.create(chatGPTUrl);
    }
}
