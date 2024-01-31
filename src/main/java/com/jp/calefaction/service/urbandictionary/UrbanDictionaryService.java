package com.jp.calefaction.service.urbandictionary;

import com.jp.calefaction.model.urbandictionary.UrbanDictionaryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UrbanDictionaryService {

    private final WebClient webClient;

    public UrbanDictionaryService(@Qualifier("urban") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<UrbanDictionaryResponse> fetchDefinitionByTerm(String term) {
        return webClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path("/define").queryParam("term", term).build())
                .retrieve()
                .bodyToMono(UrbanDictionaryResponse.class);
    }
}
