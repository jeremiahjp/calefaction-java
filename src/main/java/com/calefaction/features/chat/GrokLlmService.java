package com.calefaction.features.chat;

import com.calefaction.features.chat.model.ChatRequest;
import com.calefaction.features.chat.model.ChatResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
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
public class GrokLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(GrokLlmService.class);
    private final WebClient webClient;
    private final ImageUtil imageUtil;

    @Value("${xai.api-key:}")
    private String apiKey;

    public GrokLlmService(WebClient.Builder webClientBuilder, ImageUtil imageUtil) {
        this.imageUtil = imageUtil;
        this.webClient = webClientBuilder
                .baseUrl("https://api.x.ai/v1")
                .build();
    }

    @Override
    public Mono<ChatResponse> generateResponse(ChatRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.just(ChatResponse.builder()
                    .content("xAI API Key is missing.")
                    .provider(getProviderName())
                    .build());
        }

        return Mono.fromCallable(() -> buildRequestBody(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(requestBody -> {
                    log.info("[Grok] Sending request to /responses endpoint - Model: {}", request.getModelId());
                    return webClient.post()
                            .uri("/responses")
                            .header("Authorization", "Bearer " + apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(GrokResponsesApiResponse.class)
                            .map(response -> processResponse(response, request));
                })
                .onErrorResume(e -> {
                    log.error("Error calling Grok API", e);
                    return Mono.just(ChatResponse.builder()
                            .content("Error calling Grok: " + e.getMessage())
                            .provider(getProviderName())
                            .build());
                });
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        log.info("[Grok] Processing request - Model: {}, Prompt length: {}, Has image: {}",
                request.getModelId(), request.getPrompt().length(),
                request.getImageUrl() != null && !request.getImageUrl().isEmpty());

        String inputText = request.getPrompt();
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(Map.of("type", "web_search"));
        tools.add(Map.of("type", "x_search"));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModelId());
        requestBody.put("tools", tools);

        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            log.info("[Grok] Fetching image from URL: {}", request.getImageUrl());
            String base64Image = imageUtil.fetchImageAsBase64(request.getImageUrl());
            String mimeType = imageUtil.getMimeType(request.getImageUrl());

            if (base64Image != null) {
                List<Map<String, Object>> content = new ArrayList<>();
                content.add(Map.of("type", "input_image", "image_url", "data:" + mimeType + ";base64," + base64Image,
                        "detail", "high"));
                content.add(Map.of("type", "input_text", "text", inputText));

                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(Map.of("role", "user", "content", content));
                requestBody.put("input", messages);
            } else {
                requestBody.put("input", inputText);
            }
        } else {
            requestBody.put("input", inputText);
        }
        return requestBody;
    }

    private ChatResponse processResponse(GrokResponsesApiResponse response, ChatRequest request) {
        log.info("[Grok] Response received");
        if (response != null && response.getOutput() != null && !response.getOutput().isEmpty()) {
            StringBuilder textContent = new StringBuilder();
            for (GrokResponsesApiResponse.OutputItem item : response.getOutput()) {
                if ("message".equals(item.getType()) && item.getContent() != null) {
                    for (GrokResponsesApiResponse.ContentItem content : item.getContent()) {
                        if ("output_text".equals(content.getType()) && content.getText() != null) {
                            textContent.append(content.getText());
                        }
                    }
                }
            }

            if (response.getCitations() != null && !response.getCitations().isEmpty()) {
                textContent.append("\n\n**Sources:**\n");
                for (GrokResponsesApiResponse.Citation citation : response.getCitations()) {
                    if (citation.getUrl() != null) {
                        textContent.append("- ").append(citation.getTitle() != null ? citation.getTitle() : "Source")
                                .append(": ").append(citation.getUrl()).append("\n");
                    }
                }
            }

            GrokResponsesApiResponse.Usage usage = response.getUsage();
            return ChatResponse.builder()
                    .content(textContent.toString())
                    .provider(getProviderName())
                    .modelId(response.getModel())
                    .usage(usage != null ? ChatResponse.Usage.builder()
                            .promptTokens(usage.getInputTokens())
                            .completionTokens(usage.getOutputTokens())
                            .totalTokens(usage.getInputTokens() + usage.getOutputTokens())
                            .build() : null)
                    .build();
        }
        return ChatResponse.builder().content("No response from Grok.").provider(getProviderName()).build();
    }

    @Override
    public String getProviderName() {
        return "grok";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrokResponsesApiResponse {
        private String id;
        private String model;
        private List<OutputItem> output;
        private List<Citation> citations;
        private Usage usage;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<OutputItem> getOutput() {
            return output;
        }

        public void setOutput(List<OutputItem> output) {
            this.output = output;
        }

        public List<Citation> getCitations() {
            return citations;
        }

        public void setCitations(List<Citation> citations) {
            this.citations = citations;
        }

        public Usage getUsage() {
            return usage;
        }

        public void setUsage(Usage usage) {
            this.usage = usage;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OutputItem {
            private String type;
            private String role;
            private List<ContentItem> content;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }

            public List<ContentItem> getContent() {
                return content;
            }

            public void setContent(List<ContentItem> content) {
                this.content = content;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ContentItem {
            private String type;
            private String text;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Citation {
            private String url;
            private String title;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Usage {
            @JsonProperty("input_tokens")
            private int inputTokens;
            @JsonProperty("output_tokens")
            private int outputTokens;

            public int getInputTokens() {
                return inputTokens;
            }

            public void setInputTokens(int inputTokens) {
                this.inputTokens = inputTokens;
            }

            public int getOutputTokens() {
                return outputTokens;
            }

            public void setOutputTokens(int outputTokens) {
                this.outputTokens = outputTokens;
            }
        }
    }
}
