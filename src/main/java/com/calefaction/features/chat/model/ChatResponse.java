package com.calefaction.features.chat.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatResponse {
    String content;
    String provider;
    String modelId;
    Usage usage;
    Cost cost;

    @Value
    @Builder
    public static class Usage {
        int promptTokens;
        int completionTokens;
        int totalTokens;
    }

    @Value
    @Builder
    public static class Cost {
        double inputCost;
        double outputCost;
        double totalCost;

        public String format() {
            return String.format("$%.6f (input: $%.6f, output: $%.6f)", totalCost, inputCost, outputCost);
        }
    }
}
