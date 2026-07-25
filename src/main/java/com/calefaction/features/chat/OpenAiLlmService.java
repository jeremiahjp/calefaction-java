package com.calefaction.features.chat;

import com.calefaction.features.chat.dto.OpenAiResponse;
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

@Service
public class OpenAiLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmService.class);
    private final WebClient webClient;

    @Value("${openai.api-key:}")
    private String apiKey;

    public OpenAiLlmService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    @Override
    public Mono<ChatResponse> generateResponse(ChatRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.just(ChatResponse.builder()
                    .content("OpenAI API Key is missing.")
                    .provider(getProviderName())
                    .build());
        }

        log.info("[OpenAI] Processing request - Model: {}, Prompt length: {}, Has image: {}",
                request.getModelId(), request.getPrompt().length(),
                request.getImageUrl() != null && !request.getImageUrl().isEmpty());

        List<Object> messageContent = new java.util.ArrayList<>();
        messageContent.add(Map.of("type", "text", "text", request.getPrompt()));

        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            messageContent.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", request.getImageUrl())));
        }

        Map<String, Object> requestBody = Map.of(
                "model", request.getModelId(),
                "messages", List.of(
                        Map.of("role", "user", "content", messageContent)));

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .map(response -> {
                    if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        log.info("[OpenAI] Response received - Model: {}", response.getModel());
                        OpenAiResponse.Choice choice = response.getChoices().get(0);
                        OpenAiResponse.Usage usage = response.getUsage();

                        return ChatResponse.builder()
                                .content(choice.getMessage().getContent())
                                .provider(getProviderName())
                                .modelId(response.getModel())
                                .usage(usage != null ? ChatResponse.Usage.builder()
                                        .promptTokens(usage.getPromptTokens())
                                        .completionTokens(usage.getCompletionTokens())
                                        .totalTokens(usage.getTotalTokens())
                                        .build() : null)
                                .build();
                    }
                    return ChatResponse.builder().content("No response from OpenAI.").provider(getProviderName())
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error calling OpenAI API", e);
                    return Mono.just(ChatResponse.builder()
                            .content("Error calling OpenAI: " + e.getMessage())
                            .provider(getProviderName())
                            .build());
                });
    }

    @Override
    public String getProviderName() {
        return "openai";
    }
}
