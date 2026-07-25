package com.calefaction.features.urbandictionary;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UrbanDictionaryService {

    private static final Logger log = LoggerFactory.getLogger(UrbanDictionaryService.class);
    private final WebClient webClient;

    public UrbanDictionaryService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.urbandictionary.com/v0")
                .build();
    }

    public Mono<Definition> define(String term) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/define")
                        .queryParam("term", term)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response != null && response.containsKey("list")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) response.get("list");
                        if (!list.isEmpty()) {
                            // Get the top definition (usually the first one)
                            Map<String, Object> topDef = list.get(0);
                            String word = (String) topDef.get("word");
                            String definition = (String) topDef.get("definition");
                            String example = (String) topDef.get("example");
                            String author = (String) topDef.get("author");
                            String permalink = (String) topDef.get("permalink");

                            // Thumbs up/down might be integers
                            int thumbsUp = 0;
                            if (topDef.get("thumbs_up") instanceof Number) {
                                thumbsUp = ((Number) topDef.get("thumbs_up")).intValue();
                            }

                            return Mono.just(new Definition(word, definition, example, author, permalink, thumbsUp));
                        }
                    }
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error calling UrbanDictionary API", e))
                .onErrorResume(e -> Mono.empty());
    }

    public record Definition(String word, String definition, String example, String author, String permalink,
            int thumbsUp) {
    }
}
