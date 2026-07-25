package com.calefaction.features.weather;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenMeteoWeatherService implements WeatherService {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoWeatherService.class);
    private final WebClient webClient;

    public OpenMeteoWeatherService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
    }

    @Override
    public Mono<WeatherData> getWeather(double lat, double lon, String units) {
        String tempUnitParam = "celsius"; // Default
        String unitDisplay = "°C";

        if (units != null) {
            String u = units.toLowerCase();
            if (u.startsWith("f") || u.contains("imp")) {
                tempUnitParam = "fahrenheit";
                unitDisplay = "°F";
            }
        }

        String finalTempUnitParam = tempUnitParam;
        String finalUnitDisplay = unitDisplay;

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("current", "temperature_2m,weather_code")
                        .queryParam("temperature_unit", finalTempUnitParam)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response != null && response.containsKey("current")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> current = (Map<String, Object>) response.get("current");
                        double temp = 0.0;
                        Object tempObj = current.get("temperature_2m");
                        if (tempObj instanceof Number) {
                            temp = ((Number) tempObj).doubleValue();
                        }

                        int code = 0;
                        Object codeObj = current.get("weather_code");
                        if (codeObj instanceof Number) {
                            code = ((Number) codeObj).intValue();
                        }

                        return Mono.just(new WeatherData(temp, finalUnitDisplay, code));
                    }
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error calling OpenMeteo API", e))
                .onErrorResume(e -> Mono.empty());
    }
}
