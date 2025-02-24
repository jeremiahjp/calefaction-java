package com.jp.calefaction.slashcommands.chatgpt;

import com.jp.calefaction.model.ai.ImageCreationRequest;
import com.jp.calefaction.service.ai.ChatGPTService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class GenerateImageSubCommand {

    private final ChatGPTService chatGPTService;

    public Mono<Void> handleImageCreationCommand(
            ApplicationCommandInteractionOption subcommand, ChatInputInteractionEvent event) {
        Optional<String> queryOpt = subcommand
                .getOption("query")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        Optional<Boolean> privateOpt = subcommand
                .getOption("private")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean);

        Optional<String> sizeOpt = subcommand
                .getOption("size")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        String query = queryOpt.orElse("No query provided.");
        boolean isPrivateChat = privateOpt.orElse(false);
        String size = sizeOpt.orElse("1024x1024");

        // Message message = new Message();
        // message.setContent(query);
        // message.setRole("assistant");
        String quality = "hd";
        String style = "natural";

        ImageCreationRequest request = new ImageCreationRequest(
                "dall-e-3",
                query,
                1,
                quality,
                "url",
                size,
                style,
                event.getInteraction().getUser().getId().asString());

        ImageCreationRequest reqNew = new ImageCreationRequest();
        reqNew.setModel("dall-e-3");
        reqNew.setPrompt(query);
        log.info("Sending request with the imagecreationrequest: {}", reqNew);

        // Immediately reply so discord is happy
        return event.deferReply()
                .withEphemeral(isPrivateChat)
                .then(event.getInteractionResponse().getInitialResponse())
                .flatMap(originalResponse -> {
                    return chatGPTService
                            .getImageCreation(reqNew)
                            .flatMap(response -> {
                                // String cost = APICostCalculator.getFormattedCost(response);
                                // Choice firstChoice = response.getChoices().get(0);
                                log.info("Updating message");
                                return event.editReply(response.getUrl());
                                // .withEmbeds(response.getUrl());
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
