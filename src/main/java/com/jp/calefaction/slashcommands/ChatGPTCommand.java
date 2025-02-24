package com.jp.calefaction.slashcommands;

import com.jp.calefaction.slashcommands.chatgpt.ChatSubcommand;
import com.jp.calefaction.slashcommands.chatgpt.GenerateImageSubCommand;
import com.jp.calefaction.slashcommands.chatgpt.ImageSubCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatGPTCommand implements SlashCommand {
    private final ChatSubcommand chatSubcommand;
    private final GenerateImageSubCommand generateImageSubCommand;
    private final ImageSubCommand imageSubCommand;

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
                return chatSubcommand.handleChatSubcommand(subcommand, event);
            case "image":
                log.info("Processing image command...");
                return imageSubCommand.handleImageSubcommand(subcommand, event);
            case "generateimage":
                log.info("Processing generate image command...");
                return generateImageSubCommand.handleImageCreationCommand(subcommand, event);
            default:
                log.info("Processing unknown...");
                return event.reply().withEphemeral(true).withContent("Unknown subcommand.");
        }
    }
}
