package com.calefaction.features.chat;

import com.calefaction.features.chat.dto.GeminiResponse;
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
public class GeminiLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmService.class);
    private final WebClient webClient;
    private final ImageUtil imageUtil;

    @Value("${gemini.api-key:}")
    private String apiKey;

    public GeminiLlmService(WebClient.Builder webClientBuilder, ImageUtil imageUtil) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .build();
        this.imageUtil = imageUtil;
    }

    @Override
    public Mono<ChatResponse> generateResponse(ChatRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.just(ChatResponse.builder()
                    .content("Gemini API Key is missing.")
                    .provider(getProviderName())
                    .build());
        }

        return Mono.fromCallable(() -> buildRequestBody(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(requestBody -> webClient.post()
                        .uri("/" + request.getModelId() + ":generateContent?key=" + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(GeminiResponse.class)
                        .map(response -> {
                            if (response != null && response.getCandidates() != null
                                    && !response.getCandidates().isEmpty()) {
                                GeminiResponse.Candidate candidate = response.getCandidates().get(0);
                                GeminiResponse.UsageMetadata usage = response.getUsageMetadata();
                                String text = candidate.getContent().getParts().get(0).getText();

                                return ChatResponse.builder()
                                        .content(text)
                                        .provider(getProviderName())
                                        .modelId(request.getModelId())
                                        .usage(usage != null ? ChatResponse.Usage.builder()
                                                .promptTokens(usage.getPromptTokenCount())
                                                .completionTokens(usage.getCandidatesTokenCount())
                                                .totalTokens(usage.getTotalTokenCount())
                                                .build() : null)
                                        .build();
                            }
                            return ChatResponse.builder().content("No response from Gemini.")
                                    .provider(getProviderName()).build();
                        }))
                .onErrorResume(e -> {
                    log.error("Error calling Gemini API", e);
                    return Mono.just(ChatResponse.builder()
                            .content("Error calling Gemini: " + e.getMessage())
                            .provider(getProviderName())
                            .build());
                });
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        log.info("[Gemini] Processing request - Model: {}, Prompt length: {}, Has image: {}",
                request.getModelId(), request.getPrompt().length(),
                request.getImageUrl() != null && !request.getImageUrl().isEmpty());

        List<Map<String, Object>> parts = new java.util.ArrayList<>();
        parts.add(Map.of("text", request.getPrompt()));

        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            String base64Image = imageUtil.fetchImageAsBase64(request.getImageUrl());
            if (base64Image != null) {
                String mimeType = imageUtil.getMimeType(request.getImageUrl());
                parts.add(Map.of(
                        "inline_data", Map.of(
                                "mime_type", mimeType,
                                "data", base64Image)));
            }
        }

        return Map.of("contents", List.of(Map.of("parts", parts)));
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }
}
