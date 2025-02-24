package com.jp.calefaction.slashcommands.chatgpt;

import com.jp.calefaction.components.APICostCalculator;
import com.jp.calefaction.model.ai.ChatCompletionRequest.Message;
import com.jp.calefaction.model.ai.ChatCompletionResponse.Choice;
import com.jp.calefaction.service.ai.ChatGPTEmbedResponseService;
import com.jp.calefaction.service.ai.ChatGPTService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Attachment;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class ImageSubCommand {

    private final ChatGPTService chatGPTService;
    private final ChatGPTEmbedResponseService embedResponseService;

    public Mono<Void> handleImageSubcommand(
            ApplicationCommandInteractionOption subcommand, ChatInputInteractionEvent event) {
        Optional<String> queryOpt = subcommand
                .getOption("query")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        Optional<String> urlOpt = subcommand
                .getOption("url")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        Optional<Attachment> attachmentOpt = subcommand
                .getOption("attachment")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asAttachment);

        Optional<Boolean> privateOpt = subcommand
                .getOption("private")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean);

        String query = queryOpt.orElse("No query provided.");
        boolean isPrivateImage = privateOpt.orElse(false);

        Message message = new Message();
        message.setContent(query);
        message.setRole("assistant");

        // provided a URL
        if (urlOpt.isPresent()) {
            String imageUrl = urlOpt.get();
            return processImageRequest(event, query, imageUrl, null, isPrivateImage, message);
        }
        // provided an attachment
        else if (attachmentOpt.isPresent()) {
            Attachment imageAttachment = attachmentOpt.get();
            return processImageRequest(
                    event, query, imageAttachment.getUrl(), imageAttachment, isPrivateImage, message);
        }
        // neither
        else {
            return event.reply("You must provide either a URL or an attachment.")
                    .withEphemeral(true)
                    .then();
        }
    }

    public Mono<Void> processImageRequest(
            ChatInputInteractionEvent event,
            String query,
            String imageUrl,
            Attachment attachment,
            boolean isPrivate,
            Message message) {

        if (imageUrl.toLowerCase().endsWith("gif")) {
            return event.reply("Gifs not supported.\n"
                    + "We currently support PNG (.png), JPEG (.jpeg and .jpg), WEBP (.webp), and non-animated GIF"
                    + " (.gif).");
        }

        // Immediately reply so Discord is happy
        return event.deferReply()
                .withEphemeral(isPrivate)
                .then(event.getInteractionResponse().getInitialResponse())
                .flatMap(originalResponse -> {
                    return chatGPTService
                            .getChatCompletionWithImage(query, imageUrl)
                            .flatMap(response -> {
                                String cost = APICostCalculator.getFormattedCost(response);
                                Choice firstChoice = response.getChoices().get(0);
                                return event.editReply("Processed.")
                                        .withEmbeds(embedResponseService.embededImageResponse(
                                                query, imageUrl, firstChoice, cost));
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
