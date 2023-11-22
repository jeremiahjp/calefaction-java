package com.jp.calefaction.service;

import com.jp.calefaction.model.dogapi.DogDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DogImageService {

    private final WebClient webClient;
    private final String API_KEY = System.getenv("Dogapi");

    public DogImageService(@Qualifier("dogapi") WebClient webClient) {
        this.webClient = webClient;
    }

    public List<DogDto> fetchDogImage(String mimeType) {

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder.queryParam("mime_types", mimeType).build())
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToFlux(DogDto.class)
                .collectList()
                .block();
    }
}
