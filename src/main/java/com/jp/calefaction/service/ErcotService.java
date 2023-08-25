package com.jp.calefaction.service;

import com.jp.calefaction.model.ercot.PowerData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

public class ErcotService {

    private final WebClient webClient;
    private final String FUEL_MIX = "fuel-mix";

    @Value("${ercot.baseUrl}")
    private String baseUrl;

    public ErcotService() {
        this.webClient = WebClient.create(baseUrl);
    }

    public PowerData fetchFuelMix() {

        return webClient
                .get()
                .uri(FUEL_MIX)
                .retrieve()
                .bodyToMono(PowerData.class)
                .block();
    }
}
