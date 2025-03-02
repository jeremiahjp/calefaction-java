package com.jp.calefaction.slashcommands;

import com.jp.calefaction.service.TextGenService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TextGenCommand implements SlashCommand {

    private final TextGenService textGenService;

    public TextGenCommand(TextGenService textGenService) {
        this.textGenService = textGenService;
    }

    @Override
    public String getName() {
        return "textgen";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        return Mono.justOrEmpty(event.getOptions().stream()
                        .findFirst()
                        .flatMap(option -> option.getValue())
                        .map(ApplicationCommandInteractionOptionValue::asString))
                .map(textGenService::translateToEmoji)
                .flatMap(translatedText -> event.reply(translatedText).withEphemeral(true))
                .then();
    }
}
