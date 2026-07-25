package com.calefaction.features.weather;

import com.calefaction.features.weather.dto.OpenWeatherOneCallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenWeatherService {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherService.class);
    private final WebClient webClient;

    @Value("${openweather.api-key:}")
    private String apiKey;

    public OpenWeatherService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.openweathermap.org/data/3.0")
                .build();
    }

    public Mono<OpenWeatherOneCallResponse> getOneCall(double lat, double lon, String units) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenWeatherMap API Key is missing");
            return Mono.empty();
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/onecall")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", apiKey)
                        .queryParam("units", units) // standard, metric, imperial
                        .queryParam("exclude", "minutely") // we don't need minutely
                        .build())
                .retrieve()
                .bodyToMono(OpenWeatherOneCallResponse.class)
                .doOnError(e -> log.error("Error calling OpenWeatherMap OneCall API", e))
                .onErrorResume(e -> Mono.empty());
    }
}
