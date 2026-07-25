package com.calefaction.features.imagine;

import com.calefaction.core.MediaDownloadService;
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
public class OpenAiVideoService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiVideoService.class);
    private static final int MAX_POLL_ATTEMPTS = 30; // 30 * 10s = 5 minutes max
    private final WebClient webClient;
    private final MediaDownloadService mediaDownloadService;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.models.video:sora-2}")
    private String videoModel;

    public OpenAiVideoService(
            WebClient.Builder webClientBuilder,
            MediaDownloadService mediaDownloadService,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.mediaDownloadService = mediaDownloadService;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<String> generateVideo(String prompt, int durationSeconds, java.util.function.Consumer<Integer> attemptCallback) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API Key is missing");
            return Mono.empty();
        }

        Map<String, Object> requestBody = Map.of(
                "model", videoModel,
                "prompt", prompt,
                "duration", durationSeconds);

        return webClient.post()
                .uri("/videos")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response != null && response.containsKey("id")) {
                        String videoId = (String) response.get("id");
                        return pollForVideoResult(videoId, 1, attemptCallback);
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException ex) {
                        log.error("Error calling OpenAI Video API. Status: {}, Response: {}", ex.getStatusCode(),
                                ex.getResponseBodyAsString(), ex);
                    } else {
                        log.error("Error calling OpenAI Video API", e);
                    }
                    return Mono.error(e);
                });
    }

    private Mono<String> pollForVideoResult(String videoId, int attempt, java.util.function.Consumer<Integer> attemptCallback) {
        if (attemptCallback != null) {
            attemptCallback.accept(attempt);
        }
        return webClient.get()
                .uri("/videos/" + videoId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response != null) {
                        String status = (String) response.get("status");
                        if ("completed".equalsIgnoreCase(status)) {
                            // Assuming the video URL is returned in a 'url' field when completed
                            if (response.containsKey("url")) {
                                return Mono.just((String) response.get("url"));
                            }
                            return Mono.empty();
                        } else if ("failed".equalsIgnoreCase(status)) {
                            return Mono.error(new RuntimeException("Video generation failed."));
                        } else {
                            if (attempt >= MAX_POLL_ATTEMPTS) {
                                return Mono.error(new RuntimeException("Video generation timed out after " + attempt + " attempts."));
                            }
                            // Delay 10 seconds and poll again for Sora, as it is slower
                            return Mono.delay(java.time.Duration.ofSeconds(10))
                                    .flatMap(t -> pollForVideoResult(videoId, attempt + 1, attemptCallback));
                        }
                    }
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error polling OpenAI Video API", e));
    }

    public Mono<byte[]> downloadMedia(String url) {
        return mediaDownloadService.download(url);
    }
}
