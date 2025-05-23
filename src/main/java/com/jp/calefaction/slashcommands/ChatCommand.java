package com.jp.calefaction.slashcommands;

import com.jp.calefaction.components.APICostCalculator;
import com.jp.calefaction.config.AISystemConfig;
import com.jp.calefaction.model.ai.ChatCompletionRequest;
import com.jp.calefaction.model.ai.ChatCompletionRequest.Message;
import com.jp.calefaction.model.ai.ChatCompletionResponse.Choice;
import com.jp.calefaction.model.ai.ChatPaginationData;
import com.jp.calefaction.service.ai.ChatGPTEmbedResponseService;
import com.jp.calefaction.service.ai.ChatGPTService;
import com.jp.calefaction.service.ai.GrokChatService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ChatCommand implements SlashCommand {
    private static final String CHAT_CACHE = "chat_cache";
    private static final int MAX_TEXT_SIZE = 1950; // Discord's limit is 2000, leaving room for formatting
    private static final int MAX_EMBED_SIZE =
            4000; // Discord's embed description limit is 4096, leaving room for formatting
    private static final String BUTTON_PREV = "chat_prev";
    private static final String BUTTON_NEXT = "chat_next";

    private final ChatGPTService chatGPTService;
    private final GrokChatService grokChatService;
    private final ChatGPTEmbedResponseService chatGPTEmbedResponseService;
    private final CacheManager cacheManager;
    private ChatInputInteractionEvent interaction;

    @Value("${chatGPT.version}")
    private String gptVersion;

    @Value("${grok.model}")
    private String grokModel;

    public ChatCommand(
            ChatGPTService chatGPTService,
            GrokChatService grokChatService,
            ChatGPTEmbedResponseService chatGPTEmbedResponseService,
            CacheManager cacheManager) {
        this.chatGPTService = chatGPTService;
        this.grokChatService = grokChatService;
        this.chatGPTEmbedResponseService = chatGPTEmbedResponseService;
        this.cacheManager = cacheManager;
    }

    public ChatPaginationData getPaginationData(String interactionId) {
        Cache cache = cacheManager.getCache(CHAT_CACHE);
        if (cache != null) {
            return cache.get(interactionId, ChatPaginationData.class);
        }
        return null;
    }

    @Override
    public String getName() {
        return "chat";
    }

    private String formatResponse(String response, String model, String cost, boolean isEmbed) {
        int maxSize = isEmbed ? MAX_EMBED_SIZE : MAX_TEXT_SIZE;
        String formattedResponse = response;

        // Calculate total length including formatting
        int totalLength = formattedResponse.length();
        if (!isEmbed) {
            totalLength += 50; // Account for headers and cost info in text mode
        }

        // Only split if we exceed the limit
        if (totalLength > maxSize) {
            List<String> pages = splitIntoPages(formattedResponse, maxSize, isEmbed);
            if (!pages.isEmpty()) {
                formattedResponse = String.format(
                        "**Response from %s** (Page 1/%d)\n\n%s\n\n*Cost: %s*",
                        model, pages.size(), pages.get(0), cost);
                // Store pagination data in cache
                Cache cache = cacheManager.getCache(CHAT_CACHE);
                if (cache != null) {
                    cache.put(
                            interaction.getInteraction().getId().asString(),
                            new ChatPaginationData(
                                    interaction.getInteraction().getId().asString(),
                                    null, // Will be set after message is sent
                                    model,
                                    cost,
                                    pages,
                                    System.currentTimeMillis(),
                                    isEmbed));
                }
            }
        } else {
            // For single-page responses, format without pagination info
            formattedResponse =
                    String.format("**Response from %s**\n\n%s\n\n*Cost: %s*", model, formattedResponse, cost);
        }

        return formattedResponse;
    }

    private List<String> splitIntoPages(String content, int maxSize, boolean isEmbed) {
        List<String> pages = new ArrayList<>();
        int currentIndex = 0;
        int lookbackWindow = isEmbed ? 400 : 200; // Larger window for embeds since they can fit more content

        while (currentIndex < content.length()) {
            int endIndex = Math.min(currentIndex + maxSize, content.length());

            if (endIndex == content.length()) {
                pages.add(content.substring(currentIndex));
                break;
            }

            // Look for a good break point
            int breakPoint = findBreakPoint(content, currentIndex + maxSize - lookbackWindow, endIndex);
            if (breakPoint == -1) {
                // If no good break point found, force break at max size
                breakPoint = currentIndex + maxSize;
            }

            pages.add(content.substring(currentIndex, breakPoint).trim());
            currentIndex = breakPoint;
        }

        return pages;
    }

    private int findBreakPoint(String content, int start, int end) {
        // Look for newline first
        int newlineIndex = content.lastIndexOf('\n', end);
        if (newlineIndex > start && newlineIndex > end - 200) {
            return newlineIndex + 1;
        }

        // Look for period + space
        int periodIndex = content.lastIndexOf(". ", end);
        if (periodIndex > start && periodIndex > end - 200) {
            return periodIndex + 2;
        }

        // Look for space
        int spaceIndex = content.lastIndexOf(' ', end);
        if (spaceIndex > start && spaceIndex > end - 100) {
            return spaceIndex + 1;
        }

        return -1;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String query = event.getOption("query")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");
        String model = event.getOption("model")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("gpt-4");
        String responseType = event.getOption("response_type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("text");
        boolean isPrivate = event.getOption("private")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean)
                .orElse(false);

        interaction = event;

        // Create messages list with system message and user message
        List<Message> messages = new ArrayList<>();
        Message sysMessage = new Message();
        sysMessage.setRole("system");
        sysMessage.setContent(AISystemConfig.DISCORD_SYSTEM_MESSAGE);
        messages.add(sysMessage);

        Message userMessage = new Message();
        userMessage.setContent(query);
        userMessage.setRole("user");
        messages.add(userMessage);

        // Create the request
        ChatCompletionRequest request = new ChatCompletionRequest(model, messages);
        request.setSearchMode("on");

        return event.deferReply()
                .withEphemeral(isPrivate)
                .then(
                        model.equals(grokModel)
                                ? grokChatService.getChatCompletion(request)
                                : chatGPTService.getChatCompletion(request))
                .flatMap(response -> {
                    String cost = APICostCalculator.getFormattedCost(response);
                    String formattedResponse = formatResponse(
                            response.getChoices().get(0).getMessage().getContent(),
                            model,
                            cost,
                            responseType.equals("embed"));

                    if (responseType.equals("embed")) {
                        EmbedCreateSpec embed = chatGPTEmbedResponseService.createChatGPTEmbed(
                                query, response.getChoices().get(0), cost, formattedResponse);

                        return event.createFollowup()
                                .withEmbeds(embed)
                                .withComponents(getPaginationComponents(responseType.equals("embed")))
                                .flatMap(message -> {
                                    // Store message ID in cache
                                    Cache cache = cacheManager.getCache(CHAT_CACHE);
                                    if (cache != null) {
                                        ChatPaginationData data = cache.get(
                                                event.getInteraction().getId().asString(), ChatPaginationData.class);
                                        if (data != null) {
                                            data.setMessageId(message.getId().asString());
                                            cache.put(
                                                    event.getInteraction()
                                                            .getId()
                                                            .asString(),
                                                    data);
                                        }
                                    }
                                    return Mono.empty();
                                });
                    } else {
                        // Text response with pagination
                        return event.createFollowup()
                                .withContent(formattedResponse)
                                .withComponents(getPaginationComponents(false))
                                .flatMap(message -> {
                                    // Store message ID in cache
                                    Cache cache = cacheManager.getCache(CHAT_CACHE);
                                    if (cache != null) {
                                        ChatPaginationData data = cache.get(
                                                event.getInteraction().getId().asString(), ChatPaginationData.class);
                                        if (data != null) {
                                            data.setMessageId(message.getId().asString());
                                            cache.put(
                                                    event.getInteraction()
                                                            .getId()
                                                            .asString(),
                                                    data);
                                        }
                                    }
                                    return Mono.empty();
                                });
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error processing chat command", e);
                    return event.createFollowup("Error processing your request: " + e.getMessage())
                            .withEphemeral(true);
                })
                .then();
    }

    private ActionRow getPaginationComponents(boolean isEmbed) {
        String interactionId = interaction.getInteraction().getId().asString();
        Cache cache = cacheManager.getCache(CHAT_CACHE);
        ChatPaginationData data = cache != null ? cache.get(interactionId, ChatPaginationData.class) : null;
        boolean hasPages =
                data != null && data.getPages() != null && data.getPages().size() > 1;

        return ActionRow.of(
                Button.primary(BUTTON_PREV + ":" + interactionId, "Previous").disabled(true),
                Button.primary(BUTTON_NEXT + ":" + interactionId, "Next").disabled(!hasPages));
    }

    // Update Grok embed creation to use the formatted response
    private EmbedCreateSpec createGrokEmbed(String query, Choice choice, String cost, String formattedResponse) {
        return EmbedCreateSpec.builder()
                .color(Color.DEEP_LILAC)
                .title("Grok AI")
                .description(formattedResponse)
                .addField("Finish reason", choice.getFinish_reason(), false)
                .addField("Query", query, false)
                .addField("Cost", cost, false)
                .footer("x.ai " + grokModel, "")
                .build();
    }
}
