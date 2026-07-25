package com.calefaction.features.weather;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Primary
public class GoogleGeocodingService implements GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GoogleGeocodingService.class);

    @Value("${google.geocoding.api-key:}")
    private String apiKey;

    private GeoApiContext geoApiContext;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            geoApiContext = new GeoApiContext.Builder()
                    .apiKey(apiKey)
                    .build();
            log.info("[Google Geocoding] Initialized with API key");
        } else {
            log.warn("[Google Geocoding] API key not configured - geocoding will not work");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (geoApiContext != null) {
            geoApiContext.shutdown();
        }
    }

    @Override
    public Mono<GeoLocation> resolve(String locationQuery) {
        if (geoApiContext == null) {
            log.warn("[Google Geocoding] Cannot resolve - API not initialized");
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
            log.info("[Google Geocoding] Resolving: '{}'", locationQuery);
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, locationQuery).await();

            if (results != null && results.length > 0) {
                GeocodingResult result = results[0];
                String formattedAddress = result.formattedAddress;
                double lat = result.geometry.location.lat;
                double lng = result.geometry.location.lng;

                log.info("[Google Geocoding] Found: {} at ({}, {})", formattedAddress, lat, lng);
                return new GeoLocation(formattedAddress, lat, lng);
            }
            log.warn("[Google Geocoding] No results found for: {}", locationQuery);
            return null;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> result != null ? Mono.just(result) : Mono.empty())
                .onErrorResume(e -> {
                    log.error("[Google Geocoding] Error resolving location", e);
                    return Mono.empty();
                });
    }
}
