package com.jp.calefaction.listeners.buttons.chat;

import com.jp.calefaction.listeners.buttons.ButtonHandler;
import com.jp.calefaction.model.ai.ChatPaginationData;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("chat")
@Slf4j
@AllArgsConstructor
public class ChatPaginationHandler implements ButtonHandler {
    private static final String CHAT_CACHE = "chat_cache";
    private static final String BUTTON_PREV = "chat_prev";
    private static final String BUTTON_NEXT = "chat_next";

    private final CacheManager cacheManager;

    @Override
    public String getCustomId(ButtonInteractionEvent event) {
        return event.getCustomId();
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        Optional<Message> messageOpt = event.getMessage();
        if (messageOpt.isEmpty()) {
            log.error("Message not found for button interaction");
            return event.reply("Error: Message not found").withEphemeral(true);
        }
        String messageId = messageOpt.get().getId().asString();

        // Get the interaction ID from the button's custom ID
        String[] parts = customId.split(":");
        if (parts.length < 2) {
            log.error("Invalid button custom ID format: {}", customId);
            return event.reply("Error: Invalid button format").withEphemeral(true);
        }
        String interactionId = parts[1];

        // Get pagination data from cache
        Cache cache = cacheManager.getCache(CHAT_CACHE);
        if (cache == null) {
            log.error("Cache not found: {}", CHAT_CACHE);
            return event.reply("Error: Cache not available").withEphemeral(true);
        }

        ChatPaginationData data = cache.get(interactionId, ChatPaginationData.class);
        if (data == null) {
            log.error("No pagination data found for interaction ID: {}", interactionId);
            return event.reply("This message has expired. Please make a new request.")
                    .withEphemeral(true);
        }

        // Verify message ID matches
        if (!messageId.equals(data.getMessageId())) {
            log.error("Message ID mismatch. Expected: {}, Got: {}", data.getMessageId(), messageId);
            return event.reply("Error: Message ID mismatch").withEphemeral(true);
        }

        // Extract current page from message content
        String content = messageOpt.get().getContent();
        int currentPage = extractCurrentPage(content);

        // Update page based on button
        if (BUTTON_NEXT.equals(parts[0])) {
            currentPage++;
        } else if (BUTTON_PREV.equals(parts[0])) {
            currentPage--;
        }

        // Update the message
        String updatedContent = String.format(
                "**Response from %s** (Page %d/%d)\n\n%s\n\n*Cost: %s*",
                data.getModel(),
                currentPage + 1,
                data.getPages().size(),
                data.getPages().get(currentPage),
                data.getCost());

        // Update button states
        ActionRow buttons = ActionRow.of(
                Button.primary(BUTTON_PREV + ":" + interactionId, "Previous").disabled(currentPage == 0),
                Button.primary(BUTTON_NEXT + ":" + interactionId, "Next")
                        .disabled(currentPage == data.getPages().size() - 1));

        return event.edit(updatedContent).withComponents(buttons);
    }

    private int extractCurrentPage(String content) {
        // Extract page number from content like "Response from model (Page X/Y)"
        int startIndex = content.indexOf("(Page ") + 6;
        int endIndex = content.indexOf("/", startIndex);
        return Integer.parseInt(content.substring(startIndex, endIndex)) - 1;
    }
}
