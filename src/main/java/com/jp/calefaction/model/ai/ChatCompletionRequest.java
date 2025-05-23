package com.jp.calefaction.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatCompletionRequest {
    private String model;
    private List<Message> messages;
    // private Double temperature;
    @JsonProperty("max_tokens")
    private Integer maxTokens = 1200;
    // private Integer topP;
    // private Integer frequencyPenalty;
    // private Integer presencePenalty;
    // private String stop;
    private boolean stream = false;

    @JsonProperty("search_parameters")
    private SearchParameters searchParameters;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchParameters {
        private String mode;
    }

    public ChatCompletionRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public void setSearchMode(String mode) {
        if (this.searchParameters == null) {
            this.searchParameters = new SearchParameters();
        }
        this.searchParameters.setMode(mode);
    }
}
