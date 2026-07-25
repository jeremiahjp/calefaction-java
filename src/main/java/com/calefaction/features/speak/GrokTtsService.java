package com.calefaction.features.speak;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GrokTtsService {

    private static final Logger log = LoggerFactory.getLogger(GrokTtsService.class);
    private final WebClient webClient;

    @Value("${xai.api-key:}")
    private String apiKey;

    public GrokTtsService(WebClient.Builder webClientBuilder) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(25 * 1024 * 1024)) // 25MB buffer
                .build();

        this.webClient = webClientBuilder
                .baseUrl("https://api.x.ai/v1")
                .exchangeStrategies(strategies)
                .build();
    }

    public Mono<byte[]> generateSpeech(String text, String voice) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.error(new IllegalStateException("xAI API Key is missing."));
        }

        Map<String, Object> requestBody = Map.of(
                "text", text,
                "voice_id", voice,
                "language", "en"
        );

        log.info("[Grok TTS] Requesting speech for text of length {}, voice: {}", text.length(), voice);

        return webClient.post()
                .uri("/tts")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                    response.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("[Grok TTS] API Error {}: {}", response.statusCode(), errorBody);
                        return reactor.core.publisher.Mono.error(new RuntimeException("API Error: " + errorBody));
                    })
                )
                .bodyToMono(byte[].class)
                .doOnSuccess(bytes -> log.info("[Grok TTS] Received audio response of {} bytes", bytes != null ? bytes.length : 0))
                .doOnError(e -> log.error("[Grok TTS] Failed to generate speech", e));
    }
}
