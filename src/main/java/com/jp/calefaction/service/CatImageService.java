package com.jp.calefaction.service;

import com.jp.calefaction.model.catapi.CatDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CatImageService {

    private final WebClient webClient;
    private final String API_KEY = System.getenv("Catapi");

    public CatImageService(@Qualifier("catapi") WebClient webClient) {
        this.webClient = webClient;
    }

    public List<CatDto> fetchCatImage() {

        return webClient
                .get()
                .header("x-api-key", API_KEY)
                .retrieve()
                .bodyToFlux(CatDto.class)
                .collectList()
                .block();
    }
}
