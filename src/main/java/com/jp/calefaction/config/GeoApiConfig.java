package com.jp.calefaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.maps.GeoApiContext;

@Configuration
public class GeoApiConfig {

    private final String GEOCODE_API_KEY = System.getenv("GeocodingAPI");
    
    @Bean
    GeoApiContext createMapsBean() {
       return new GeoApiContext.Builder()
        .apiKey(GEOCODE_API_KEY)
        .build();
    }
}
