package com.calefaction.features.solaredge;

import com.calefaction.features.solaredge.dto.*;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class SolarEdgeService {

    private static final Logger log = LoggerFactory.getLogger(SolarEdgeService.class);
    private final WebClient webClient;

    @Value("${solaredge.api-key:}")
    private String apiKey;

    @Value("${solaredge.site-id:}")
    private String siteId;

    public SolarEdgeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://monitoringapi.solaredge.com")
                .build();
    }

    public Mono<OverviewResponse> getOverview() {
        log.info("Requesting Overview for siteId: {}", siteId);
        if (apiKey == null || apiKey.isEmpty() || siteId == null || siteId.isEmpty()) {
            log.warn("SolarEdge API Key or Site ID is missing");
            return Mono.empty();
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/overview")
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(OverviewResponse.class)
                .doOnSuccess(response -> log.info("Received Overview: {}", response))
                .doOnError(e -> log.error("Error calling SolarEdge API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<DetailsResponse> getDetails() {
        log.info("Requesting Details for siteId: {}", siteId);
        if (apiKey == null || siteId == null)
            return Mono.empty();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/details")
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(DetailsResponse.class)
                .doOnSuccess(response -> log.info("Received Details: {}", response))
                .doOnError(e -> log.error("Error calling SolarEdge Details API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<EnergyResponse> getEnergy(String startDate, String endDate, String timeUnit) {
        log.info("Requesting Energy for siteId: {}, start: {}, end: {}, unit: {}", siteId, startDate, endDate,
                timeUnit);
        if (apiKey == null || siteId == null)
            return Mono.empty();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/energy")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .queryParam("timeUnit", timeUnit)
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(EnergyResponse.class)
                .doOnSuccess(response -> log.info("Received Energy: {}", response))
                .doOnError(e -> log.error("Error calling SolarEdge Energy API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<PowerResponse> getPower(String startTime, String endTime) {
        log.info("Requesting Power for siteId: {}, start: {}, end: {}", siteId, startTime, endTime);
        if (apiKey == null || siteId == null)
            return Mono.empty();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/power")
                        .queryParam("startTime", startTime)
                        .queryParam("endTime", endTime)
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(PowerResponse.class)
                .doOnSuccess(response -> log.info("Received Power: {}", response))
                .doOnError(e -> log.error("Error calling SolarEdge Power API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<PowerFlowResponse> getCurrentPowerFlow() {
        log.info("Requesting Power Flow for siteId: {}", siteId);
        if (apiKey == null || siteId == null)
            return Mono.empty();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/currentPowerFlow")
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(PowerFlowResponse.class)
                .doOnSuccess(response -> log.info("Received Power Flow: {}", response))
                .doOnError(e -> log.error("Error calling SolarEdge Current Power Flow API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Optional<InventoryResponse> getStorageData(String startTime, String endTime) {
        // Kept as Optional since not implemented or used in Command yet, matching
        // previous state
        return Optional.empty();
    }

    public Mono<java.util.Map<String, Object>> getStorageDataMap(String startTime, String endTime) {
        if (apiKey == null || siteId == null)
            return Mono.empty();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/storageData")
                        .queryParam("startTime", startTime)
                        .queryParam("endTime", endTime)
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<java.util.Map<String, Object>>() {
                })
                .doOnError(e -> log.error("Error calling SolarEdge Storage Data API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<InventoryResponse> getInventory() {
        log.info("Requesting Inventory for siteId: {}", siteId);
        if (apiKey == null || siteId == null)
            return Mono.empty();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/inventory")
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(InventoryResponse.class)
                .doOnSuccess(response -> log.info("Received Inventory: {}", response))
                .doOnError(e -> log.error("Error calling SolarEdge Inventory API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<EnvBenefitsResponse> getEnvBenefits() {
        log.info("Requesting Env Benefits for siteId: {}", siteId);
        if (apiKey == null || siteId == null)
            return Mono.empty();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/{siteId}/envBenefits")
                        .queryParam("systemUnits", "Imperial")
                        .queryParam("api_key", apiKey)
                        .build(siteId))
                .retrieve()
                .bodyToMono(EnvBenefitsResponse.class)
                .doOnSuccess(response -> log.info("Received Env Benefits: {}", response))
                .doOnError(e -> log.error("Error calling SolarEdge Env Benefits API", e))
                .onErrorResume(e -> Mono.empty());
    }
}
