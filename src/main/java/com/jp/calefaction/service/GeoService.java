package com.jp.calefaction.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class GeoService {
    
    private final GeoApiContext geoApiContext;

    public GeocodingResult[] getGeoResults(String address) throws ApiException, InterruptedException, IOException {
        log.info("Fetching geocode results for {}", address);
        GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
        return results;
    }
}
