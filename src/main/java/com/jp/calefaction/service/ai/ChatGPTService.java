package com.jp.calefaction.service.ai;

import com.jp.calefaction.model.ai.ChatCompletionRequest;
import com.jp.calefaction.model.ai.ChatCompletionResponse;
import com.jp.calefaction.model.ai.ModerationResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ChatGPTService {

    private final WebClient webClient;
    private static final String COMPLETIONS_URI = "/chat/completions";
    private static final String MODERATIONS_URI = "/moderations";

    private final String CHAT_GPI_API_KEY = System.getenv("chatGPTApiKey");

    public ChatGPTService(@Qualifier("chatgpt") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ChatCompletionResponse> getChatCompletion(ChatCompletionRequest request) {
        log.info("Sending the request object {}", request);
        return webClient
                .post()
                .uri(COMPLETIONS_URI)
                .header("Authorization", "Bearer " + CHAT_GPI_API_KEY)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class);
    }

    public Mono<ModerationResponse> getChatModeration(String input) {
        Map<String, String> body = Map.of("input", input);
        log.info("Sending the body: {}", body);
        return webClient
                .post()
                .uri(MODERATIONS_URI)
                .header("Authorization", "Bearer " + CHAT_GPI_API_KEY)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ModerationResponse.class);
    }
}
