package com.jp.calefaction.service.ai;

import com.jp.calefaction.config.AISystemConfig;
import com.jp.calefaction.model.ai.ChatCompletionRequest;
import com.jp.calefaction.model.ai.ChatCompletionRequest.Message;
import com.jp.calefaction.model.ai.ChatCompletionResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class GrokChatService {
    private final WebClient webClient;
    private static final String GROK_API_URL = "https://api.x.ai/v1/chat/completions";
    private final String GROK_API_KEY = System.getenv("XAI_API_KEY");

    @Value("${grok.model}")
    private String grokModel;

    public GrokChatService(@Qualifier("grokWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ChatCompletionResponse> getChatCompletion(ChatCompletionRequest request) {
        log.info("Sending request to Grok API with structure: {}", request);
        // Log the messages array specifically
        if (request.getMessages() != null) {
            request.getMessages()
                    .forEach(msg -> log.info("Message - role: {}, content: {}", msg.getRole(), msg.getContent()));
        }

        // Set model if not already set
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel(grokModel);
        }

        return webClient
                .post()
                .uri(GROK_API_URL)
                .header("Authorization", "Bearer " + GROK_API_KEY)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("API Error Response: {}", errorBody);
                            return Mono.error(new RuntimeException("API Error: " + errorBody));
                        }))
                .bodyToMono(ChatCompletionResponse.class);
    }

    /**
     * Get chat completion with a system message that defines the AI's behavior
     * @param query The user's query
     * @param customSystemMessage Optional custom system message. If null, uses default rules
     * @return Chat completion response
     */
    public Mono<ChatCompletionResponse> getChatCompletionWithSystemMessage(String query, String customSystemMessage) {
        List<Message> messages = new ArrayList<>();

        // Add system message
        Message sysMessage = new Message();
        sysMessage.setRole("system");
        sysMessage.setContent(
                customSystemMessage != null ? customSystemMessage : AISystemConfig.DISCORD_SYSTEM_MESSAGE);
        messages.add(sysMessage);

        // Add user message
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent(query);
        messages.add(userMessage);

        // Create request with the Grok model
        ChatCompletionRequest request = new ChatCompletionRequest(grokModel, messages);
        request.setSearchMode("on");

        return getChatCompletion(request);
    }

    /**
     * Get chat completion with default system message
     * @param query The user's query
     * @return Chat completion response
     */
    public Mono<ChatCompletionResponse> getChatCompletionWithDefaultRules(String query) {
        return getChatCompletionWithSystemMessage(query, null);
    }
}
