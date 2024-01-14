package com.jp.calefaction.service;

import com.jp.calefaction.model.weather.WeatherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class WeatherService {

    private final WebClient webClient;
    private static final String weatherURI = "/onecall";
    private final String API_KEY = System.getenv("OpenWeatherAPI");

    public WeatherService(@Qualifier("OpenWeather") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * This does not use a cache and will always fetch a new WeatherData
     * @param locationData
     * @param unit
     * @return
     */
    public WeatherData getOpenWeatherFromCoordinates(double lat, double lng, String unit) {
        log.info("Fetching weather from openweatherapi");
        WeatherData model = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(weatherURI)
                        .queryParam("lat", lat)
                        .queryParam("lon", lng)
                        .queryParam("appid", API_KEY)
                        .queryParam("units", unit)
                        .build())
                .retrieve()
                .bodyToMono(WeatherData.class)
                .block();

        model.setAddress(model.getAddress());
        model.setIndex(0);
        model.setUnit(unit);
        return model;
    }

    @Cacheable(value = "openweather_cache", key = "#cacheKey")
    public WeatherData getOpenWeatherFromCoordinates(double lat, double lng, String unit, String cacheKey) {
        log.info("Cache miss for location {} in openweather_cache", cacheKey);
        return updateCache(cacheKey, getOpenWeatherFromCoordinates(lat, lng, unit));
    }

    @CachePut(value = "openweather_cache", key = "#cacheKey")
    public WeatherData updateCache(String cacheKey, WeatherData cachedObject) {
        log.info("Caching entry for {}", cacheKey);
        return cachedObject;
    }

    @CacheEvict(value = "openweather_cache", key = "#cacheKey")
    public void evictOpenWeatherCache(String cacheKey) {
        log.info("Evicting from cache - {}", cacheKey);
    }
}
