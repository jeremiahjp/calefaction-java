package com.jp.calefaction.commands;

import com.jp.calefaction.components.APICostCalculator;
import com.jp.calefaction.model.ai.ChatCompletionRequest;
import com.jp.calefaction.model.ai.ChatCompletionRequest.Message;
import com.jp.calefaction.model.ai.ChatCompletionResponse.Choice;
import com.jp.calefaction.model.ai.ImageCreationRequest;
import com.jp.calefaction.service.ai.ChatGPTEmbesResponseService;
import com.jp.calefaction.service.ai.ChatGPTService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Attachment;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatGPTCommand implements SlashCommand {

    private final ChatGPTService chatGPTService;
    private final ChatGPTEmbesResponseService embedResponseService;

    @Value("${chatGPT.version}")
    private String chatGPTVersion;

    @Override
    public String getName() {
        return "chatgpt";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        log.info("inside the chatGPT handle query");

        ApplicationCommandInteractionOption subcommand = event.getOptions().get(0);

        switch (subcommand.getName()) {
            case "chat":
                log.info("Processing chat command...");
                return handleChatSubcommand(subcommand, event);
            case "image":
                log.info("Processing image command...");
                return handleImageSubcommand(subcommand, event);
            case "generateimage":
                log.info("Processing generate image command...");
                return handleImageCreationCommand(subcommand, event);
            default:
                log.info("Processing unknown...");
                return event.reply().withEphemeral(true).withContent("Unknown subcommand.");
        }
    }

    private Mono<Void> handleImageCreationCommand(
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

        Message message = new Message();
        message.setContent(query);
        message.setRole("assistant");
        String quality = "hd";
        String style = "vivid";

        ImageCreationRequest request = new ImageCreationRequest(
                "dall-e-3",
                query,
                1,
                quality,
                "url",
                size,
                style,
                event.getInteraction().getUser().getId().asString());

        // Immediately reply with "Processing..."
        return event.reply("Processing...")
                .withEphemeral(isPrivateChat)
                .then(event.getInteractionResponse().getInitialResponse())
                .flatMap(originalResponse -> {
                    return chatGPTService
                            .getImageCreation(request)
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

    private Mono<Void> handleVoiceSubCommand(
            ApplicationCommandInteractionOption subcommand, ChatInputInteractionEvent event) {
        // TODO: Fix implementation
        return null;
        // Optional<String> queryOpt = subcommand
        //         .getOption("query")
        //         .flatMap(ApplicationCommandInteractionOption::getValue)
        //         .map(ApplicationCommandInteractionOptionValue::asString);

        // Optional<Boolean> privateOpt = subcommand
        //         .getOption("private")
        //         .flatMap(ApplicationCommandInteractionOption::getValue)
        //         .map(ApplicationCommandInteractionOptionValue::asBoolean);

        // String query = queryOpt.orElse("No query provided.");
        // boolean isPrivateChat = privateOpt.orElse(false);

        // Message message = new Message();
        // message.setContent(query);
        // message.setRole("assistant");

        // ChatSpeechRequest request = new ChatSpeechRequest("tts-1-hd", query, "nova", "opus", 1.0);

        // // Immediately reply with "Processing..."
        // return event.reply("Processing...")
        //         .withEphemeral(isPrivateChat)
        //         .then(event.getInteractionResponse().getInitialResponse())
        //         .flatMap(originalResponse -> {
        //             return chatGPTService
        //                     .getSpeech(request)
        //                     .flatMap(response -> {
        //                         String cost = APICostCalculator.getFormattedCost(response);
        //                         Choice firstChoice = response.getChoices().get(0);
        //                         log.info("Updating message");
        //                         return event.editReply("Processed.")
        //                                 .withEmbeds(embedResponseService.createChatGPTEmbed(query, firstChoice,
        // cost));
        //                     })
        //                     .onErrorResume(e -> {
        //                         log.error("Error occurred while processing: ", e);
        //                         return event.editReply(
        //                                 "An error occurred while processing your request. Please try again later.");
        //                     });
        //         })
        //         .then();
    }

    private Mono<Void> handleChatSubcommand(
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

        ChatCompletionRequest request = new ChatCompletionRequest(chatGPTVersion, List.of(message));

        // Immediately reply with "Processing..."
        return event.reply("Processing...")
                .withEphemeral(isPrivateChat)
                .then(event.getInteractionResponse().getInitialResponse())
                .flatMap(originalResponse -> {
                    return chatGPTService
                            .getChatCompletion(request)
                            .flatMap(response -> {
                                String cost = APICostCalculator.getFormattedCost(response);
                                Choice firstChoice = response.getChoices().get(0);
                                log.info("Updating message");
                                return event.editReply("Processed.")
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

    private Mono<Void> handleImageSubcommand(
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

        // Immediately reply with "Processing..."
        return event.reply("Processing...")
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
