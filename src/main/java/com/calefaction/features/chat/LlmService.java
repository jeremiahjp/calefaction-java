package com.calefaction.features.chat;

import com.calefaction.features.chat.model.ChatRequest;
import com.calefaction.features.chat.model.ChatResponse;
import reactor.core.publisher.Mono;

public interface LlmService {
    Mono<ChatResponse> generateResponse(ChatRequest request);

    String getProviderName();
}
