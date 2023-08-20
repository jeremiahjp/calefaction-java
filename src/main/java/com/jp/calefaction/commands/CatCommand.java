package com.jp.calefaction.commands;

import com.jp.calefaction.model.catapi.CatDto;
import com.jp.calefaction.service.CatImageService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
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

        List<CatDto> catDto = catImageService.fetchCatImage();

        return event.reply().withEmbeds(createEmbed(catDto.get(0)));
    }

    public EmbedCreateSpec createEmbed(CatDto catDto) {
        return EmbedCreateSpec.builder()
                .title("Heres a cat")
                .image(catDto.getUrl())
                .build();
    }
}
