package com.calefaction.features.imagine;

import com.calefaction.core.MediaDownloadService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class GrokImageService {

    private static final Logger log = LoggerFactory.getLogger(GrokImageService.class);
    private static final int MAX_POLL_ATTEMPTS = 60; // 60 * 5s = 5 minutes max
    private final WebClient webClient;
    private final MediaDownloadService mediaDownloadService;

    @Value("${xai.api-key:}")
    private String apiKey;

    @Value("${xai.models.image:grok-imagine-image}")
    private String imageModel;

    @Value("${xai.models.video:grok-imagine-video}")
    private String videoModel;

    public GrokImageService(
            WebClient.Builder webClientBuilder,
            MediaDownloadService mediaDownloadService,
            @Value("${xai.base-url:https://api.x.ai/v1}") String baseUrl) {
        this.mediaDownloadService = mediaDownloadService;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<String> generateImage(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("xAI API Key is missing");
            return Mono.empty();
        }

        Map<String, Object> requestBody = Map.of(
                "model", imageModel,
                "prompt", prompt,
                "n", 1,
                "response_format", "url");

        return webClient.post()
                .uri("/images/generations")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response != null && response.containsKey("data")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                        if (!data.isEmpty()) {
                            String url = (String) data.get(0).get("url");
                            if (url != null)
                                return Mono.just(url);
                        }
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException ex) {
                        log.error("Error calling Grok Image API. Status: {}, Response: {}", ex.getStatusCode(),
                                ex.getResponseBodyAsString(), ex);
                    } else {
                        log.error("Error calling Grok Image API", e);
                    }
                    return Mono.error(e);
                });
    }

    public Mono<String> generateVideo(String prompt, int durationSeconds, java.util.function.Consumer<Integer> attemptCallback) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("xAI API Key is missing");
            return Mono.empty();
        }

        Map<String, Object> requestBody = Map.of(
                "model", videoModel,
                "prompt", prompt,
                "duration", durationSeconds);

        return webClient.post()
                .uri("/videos/generations")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response != null && response.containsKey("request_id")) {
                        String requestId = (String) response.get("request_id");
                        return pollForVideoResult(requestId, 1, attemptCallback);
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException ex) {
                        log.error("Error calling Grok Video API. Status: {}, Response: {}", ex.getStatusCode(),
                                ex.getResponseBodyAsString(), ex);
                    } else {
                        log.error("Error calling Grok Video API", e);
                    }
                    return Mono.error(e);
                });
    }

    private Mono<String> pollForVideoResult(String requestId, int attempt, java.util.function.Consumer<Integer> attemptCallback) {
        if (attemptCallback != null) {
            attemptCallback.accept(attempt);
        }
        return webClient.get()
                .uri("/videos/" + requestId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response != null) {
                        log.debug("Raw poll response: {}", response);
                        String status = (String) response.get("status");
                        if ("done".equalsIgnoreCase(status) || response.containsKey("video")) {
                            log.debug("Video generation completed (or video object present) for request: {}",
                                    requestId);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> videoObj = (Map<String, Object>) response.get("video");
                            if (videoObj != null && videoObj.containsKey("url")) {
                                return Mono.just((String) videoObj.get("url"));
                            }
                            log.error("Video URL not found in completed response for request: {}", requestId);
                            return Mono.empty();
                        } else if ("expired".equalsIgnoreCase(status)) {
                            log.warn("Video generation expired for request: {}", requestId);
                            return Mono.error(new RuntimeException("Video generation expired."));
                        } else if ("pending".equalsIgnoreCase(status)) {
                            if (attempt >= MAX_POLL_ATTEMPTS) {
                                return Mono.error(new RuntimeException("Video generation timed out after " + attempt + " attempts."));
                            }
                            log.debug("Video generation pending for request: {}", requestId);
                            // Delay 5 seconds and poll again
                            return Mono.delay(java.time.Duration.ofSeconds(5))
                                    .flatMap(t -> pollForVideoResult(requestId, attempt + 1, attemptCallback));
                        } else {
                            log.warn("Unknown status received during polling: {}", status);
                        }
                    } else {
                        log.warn("Poll response was null for request: {}", requestId);
                    }
                    return Mono.empty();
                })
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException ex) {
                        log.error("Error polling Grok Video API. Status: {}, Response: {}", ex.getStatusCode(),
                                ex.getResponseBodyAsString(), ex);
                    } else {
                        log.error("Error polling Grok Video API", e);
                    }
                });
    }

    public Mono<byte[]> downloadMedia(String url) {
        return mediaDownloadService.download(url);
    }
}
