package com.jp.calefaction.commands;

import com.jp.calefaction.model.catapi.CatDto;
import com.jp.calefaction.service.CatImageService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CatCommand implements SlashCommand {

    private final CatImageService catImageService;

    public CatCommand(CatImageService catImageService) {
        this.catImageService = catImageService;
    }

    @Override
    public String getName() {
        return "cat";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        Optional<String> imageType = event.getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        imageType.ifPresentOrElse(
                value -> {
                    log.info("gif was present");
                    List<CatDto> catDto = catImageService.fetchCatImage(imageType.get());
                    event.reply().withEmbeds(createEmbed(catDto.get(0)));
                },
                () -> {
                    log.info("type not specified");
                    List<CatDto> catDto = catImageService.fetchCatImage("");
                    event.reply().withEmbeds(createEmbed(catDto.get(0)));
                });
        return Mono.empty();
    }

    public EmbedCreateSpec createEmbed(CatDto catDto) {
        return EmbedCreateSpec.builder()
                .title("Heres a cat")
                .image(catDto.getUrl())
                .build();
    }
}
