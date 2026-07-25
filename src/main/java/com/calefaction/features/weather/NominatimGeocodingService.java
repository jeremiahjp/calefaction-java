package com.calefaction.features.weather;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class NominatimGeocodingService implements GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingService.class);
    private final WebClient webClient;

    public NominatimGeocodingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "CalefactionBot/1.0") // Required by Nominatim
                .build();
    }

    @Override
    public Mono<GeoLocation> resolve(String locationQuery) {
        log.info("[Geocoding] Resolving location: '{}'", locationQuery);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", locationQuery)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .flatMap(response -> {
                    if (response != null && !response.isEmpty()) {
                        Map<String, Object> firstResult = response.get(0);
                        String displayName = (String) firstResult.get("display_name");
                        double lat = Double.parseDouble((String) firstResult.get("lat"));
                        double lon = Double.parseDouble((String) firstResult.get("lon"));
                        log.info("[Geocoding] Found: {}", displayName);
                        return Mono.just(new GeoLocation(displayName, lat, lon));
                    }
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error calling Nominatim API", e))
                .onErrorResume(e -> Mono.empty());
    }
}
