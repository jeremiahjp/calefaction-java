package com.jp.calefaction.service.ai;

import com.jp.calefaction.model.ai.ChatCompletionRequest;
import com.jp.calefaction.model.ai.ChatCompletionResponse;
import com.jp.calefaction.model.ai.ChatSpeechRequest;
import com.jp.calefaction.model.ai.ImageCreationRequest;
import com.jp.calefaction.model.ai.ImageCreationResponse;
import com.jp.calefaction.model.ai.ModerationResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ChatGPTService {

    private final WebClient webClient;
    private static final String COMPLETIONS_URI = "/chat/completions";
    private static final String MODERATIONS_URI = "/moderations";
    private static final String SPEECH_URI = "/audio/speech";
    private static final String IMAGE_GEN_URI = "/images/generations";

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

    public Mono<ImageCreationResponse> getImageCreation(ImageCreationRequest request) {
        log.info("Sending the request object {}", request);
        return webClient
                .post()
                .uri(IMAGE_GEN_URI)
                .header("Authorization", "Bearer " + CHAT_GPI_API_KEY)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ImageCreationResponse.class);
    }

    public Mono<Resource> getSpeech(ChatSpeechRequest request) {

        return webClient
                .post()
                .uri(SPEECH_URI)
                .header("Authorization", "Bearer " + CHAT_GPI_API_KEY)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Resource.class);
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

    public Mono<ChatCompletionResponse> getChatCompletionWithImage(String textInput, String imageUrl) {
        Map<String, String> body = Map.of("input", textInput);
        log.info("Sending the body: {}", body);

        Map<String, Object> payload = Map.of(
                "model",
                "gpt-4o",
                "messages",
                new Object[] {
                    Map.of("role", "user", "content", new Object[] {
                        Map.of("type", "text", "text", textInput),
                        Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                    })
                },
                "max_tokens",
                300);

        return webClient
                .post()
                .uri(COMPLETIONS_URI)
                .header("Authorization", "Bearer " + CHAT_GPI_API_KEY)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class);
    }
}
