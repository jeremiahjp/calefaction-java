package com.jp.calefaction.commands;

import com.jp.calefaction.service.responses.embed.UrbanDictionaryEmbedResponse;
import com.jp.calefaction.service.urbandictionary.UrbanDictionaryService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class UrbanDictionary implements SlashCommand {

    private final UrbanDictionaryService urbanDictionaryService;
    private final UrbanDictionaryEmbedResponse urbanDictionaryEmbedResponse;

    @Override
    public String getName() {
        return "urbandictionary";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        Optional<String> word = event.getOption("word")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        if (word.isEmpty()) {
            return event.reply("You need to provide a word.").withEphemeral(true);
        }
        log.info("Urban dictionary word queried was {}", word.get());
        return urbanDictionaryService
                .fetchDefinitionByTerm(word.get())
                .flatMap(response -> {
                    if (response.getList().isEmpty()) {
                        return event.reply("No definition found for the word: " + word.get())
                                .withEphemeral(true);
                    }
                    return event.reply()
                            .withEmbeds(urbanDictionaryEmbedResponse.createEmbedResponse(response))
                            .then();
                })
                .then();
    }
}
