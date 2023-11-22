package com.jp.calefaction.commands;

import com.jp.calefaction.model.dogapi.DogDto;
import com.jp.calefaction.service.DogImageService;
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
public class DogCommand implements SlashCommand {

    private final DogImageService dogImageService;

    public DogCommand(DogImageService dogImageService) {
        this.dogImageService = dogImageService;
    }

    @Override
    public String getName() {
        return "dog";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        Optional<String> imageType = event.getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);
        if (imageType.isPresent()) {
            log.info("gif was present");
            List<DogDto> dogDto = dogImageService.fetchDogImage(imageType.get());
            return event.reply().withEmbeds(createEmbed(dogDto.get(0)));
        } else {
            log.info("type not specified");
            List<DogDto> dogDto = dogImageService.fetchDogImage("");
            return event.reply().withEmbeds(createEmbed(dogDto.get(0)));
        }
    }

    public EmbedCreateSpec createEmbed(DogDto dogDto) {
        return EmbedCreateSpec.builder()
                .title("Heres a doggo")
                .image(dogDto.getUrl())
                .build();
    }
}
