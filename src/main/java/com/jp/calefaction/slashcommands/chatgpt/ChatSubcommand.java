package com.jp.calefaction.slashcommands.chatgpt;

import com.jp.calefaction.components.APICostCalculator;
import com.jp.calefaction.model.ai.ChatCompletionRequest;
import com.jp.calefaction.model.ai.ChatCompletionRequest.Message;
import com.jp.calefaction.model.ai.ChatCompletionResponse.Choice;
import com.jp.calefaction.service.ai.ChatGPTEmbedResponseService;
import com.jp.calefaction.service.ai.ChatGPTService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class ChatSubcommand {

    private final ChatGPTService chatGPTService;
    private final ChatGPTEmbedResponseService embedResponseService;

    public Mono<Void> handleChatSubcommand(
            ApplicationCommandInteractionOption subcommand, ChatInputInteractionEvent event) {
        Optional<String> queryOpt = subcommand
                .getOption("query")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        Optional<Boolean> privateOpt = subcommand
                .getOption("private")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean);

        String query = queryOpt.orElse("No query provided.");
        boolean isPrivateChat = privateOpt.orElse(false);

        Message message = new Message();
        message.setContent(query);
        message.setRole("assistant");

        ChatCompletionRequest request = new ChatCompletionRequest("gpt-4o", List.of(message));

        // Immediately reply so discord is happy
        return event.deferReply()
                .withEphemeral(isPrivateChat)
                .then(event.getInteractionResponse().getInitialResponse())
                .flatMap(originalResponse -> {
                    return chatGPTService
                            .getChatCompletion(request)
                            .flatMap(response -> {
                                String cost = APICostCalculator.getFormattedCost(response);
                                Choice firstChoice = response.getChoices().get(0);
                                log.info("Updating message");
                                return event.editReply()
                                        .withEmbeds(embedResponseService.createChatGPTEmbed(query, firstChoice, cost));
                            })
                            .onErrorResume(e -> {
                                log.error("Error occurred while processing: ", e);
                                return event.editReply(
                                        "An error occurred while processing your request. Please try again later.");
                            });
                })
                .then();
    }
}
