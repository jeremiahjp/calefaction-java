package com.calefaction.features.weather;

import reactor.core.publisher.Mono;

public interface GeocodingService {
    Mono<GeoLocation> resolve(String locationQuery);

    record GeoLocation(String name, double latitude, double longitude) {
    }
}
