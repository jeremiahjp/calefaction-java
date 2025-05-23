package com.jp.calefaction.model.ai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatPaginationData {
    private String interactionId; // Original command interaction ID
    private String messageId; // Bot's response message ID
    private String model;
    private String cost;
    private List<String> pages;
    private long timestamp;
    private boolean isEmbed; // Whether this is an embed response

    // Constructor without isEmbed for backward compatibility
    public ChatPaginationData(
            String interactionId, String messageId, String model, String cost, List<String> pages, long timestamp) {
        this.interactionId = interactionId;
        this.messageId = messageId;
        this.model = model;
        this.cost = cost;
        this.pages = pages;
        this.timestamp = timestamp;
        this.isEmbed = false; // Default to text response
    }
}
