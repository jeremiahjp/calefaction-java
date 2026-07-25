package com.calefaction.features.chat.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatRequest {
    String prompt;
    String modelId;
    String imageUrl;
}
