package com.calefaction.features.weather;

import reactor.core.publisher.Mono;

public interface WeatherService {
    Mono<WeatherData> getWeather(double lat, double lon, String units);

    record WeatherData(double temperature, String tempUnit, int weatherCode) {
    }
}
