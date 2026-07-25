package com.calefaction.features.chat;

import com.calefaction.features.chat.dto.ClaudeResponse;
import com.calefaction.features.chat.model.ChatRequest;
import com.calefaction.features.chat.model.ChatResponse;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ClaudeLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeLlmService.class);
    private final WebClient webClient;
    private final ImageUtil imageUtil;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.max-tokens:4096}")
    private int maxTokens;

    public ClaudeLlmService(WebClient.Builder webClientBuilder, ImageUtil imageUtil) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com/v1")
                .build();
        this.imageUtil = imageUtil;
    }

    @Override
    public Mono<ChatResponse> generateResponse(ChatRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.just(ChatResponse.builder()
                    .content("Anthropic API Key is missing.")
                    .provider(getProviderName())
                    .build());
        }

        return Mono.fromCallable(() -> buildRequestBody(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(requestBody -> webClient.post()
                        .uri("/messages")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(ClaudeResponse.class)
                        .map(response -> {
                            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                                ClaudeResponse.Content content = response.getContent().get(0);
                                ClaudeResponse.Usage usage = response.getUsage();

                                return ChatResponse.builder()
                                        .content(content.getText())
                                        .provider(getProviderName())
                                        .modelId(response.getModel())
                                        .usage(usage != null ? ChatResponse.Usage.builder()
                                                .promptTokens(usage.getInputTokens())
                                                .completionTokens(usage.getOutputTokens())
                                                .totalTokens(usage.getInputTokens() + usage.getOutputTokens())
                                                .build() : null)
                                        .build();
                            }
                            return ChatResponse.builder().content("No response from Claude.")
                                    .provider(getProviderName()).build();
                        }))
                .onErrorResume(e -> {
                    log.error("Error calling Anthropic API", e);
                    return Mono.just(ChatResponse.builder()
                            .content("Error calling Claude: " + e.getMessage())
                            .provider(getProviderName())
                            .build());
                });
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        log.info("[Claude] Processing request - Model: {}, Prompt length: {}, Has image: {}",
                request.getModelId(), request.getPrompt().length(),
                request.getImageUrl() != null && !request.getImageUrl().isEmpty());

        List<Map<String, Object>> contentBlocks = new java.util.ArrayList<>();
        contentBlocks.add(Map.of("type", "text", "text", request.getPrompt()));

        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            String base64Image = imageUtil.fetchImageAsBase64(request.getImageUrl());
            if (base64Image != null) {
                String mimeType = imageUtil.getMimeType(request.getImageUrl());
                contentBlocks.add(Map.of(
                        "type", "image",
                        "source", Map.of(
                                "type", "base64",
                                "media_type", mimeType,
                                "data", base64Image)));
            }
        }

        return Map.of(
                "model", request.getModelId(),
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "user", "content", contentBlocks)));
    }

    @Override
    public String getProviderName() {
        return "claude";
    }
}
