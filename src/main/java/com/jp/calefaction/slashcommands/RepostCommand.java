package com.jp.calefaction.slashcommands;

import com.jp.calefaction.entity.OriginalMessages;
import com.jp.calefaction.service.RepostEmbedService;
import com.jp.calefaction.service.RepostService;
import com.jp.calefaction.service.repost.RepostCountService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class RepostCommand implements SlashCommand {

    private final RepostService repostService;
    private final RepostEmbedService repostEmbedService;
    private final RepostCountService repostCountService;

    @Override
    public String getName() {
        return "repost";
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String mainCommand = event.getCommandName();
        log.info("Received command name: {}", mainCommand);

        // Convert the options to a Flux and process each option reactively
        return Flux.fromIterable(event.getOptions())
                .flatMap(option -> {
                    if ("check".equals(option.getName())) {
                        return handleCheckCommand(
                                        event.getInteraction()
                                                .getGuildId()
                                                .get()
                                                .asString(),
                                        option)
                                .flatMap(v -> event.reply()
                                        .withEmbeds(repostEmbedService.createRepostCheckEmbed(v))
                                        .withEphemeral(true))
                                .switchIfEmpty(
                                        event.reply("You are clear to post. ✅").withEphemeral(true));
                    } else if ("top".equals(option.getName())) {
                        log.info("Top command");
                        return event.reply("This is not implemented yet.")
                                .withEphemeral(true); // replace with your actual response
                    } else {
                        return Mono.empty();
                    }
                })
                .then();
    }

    private Mono<OriginalMessages> handleCheckCommand(String guildId, ApplicationCommandInteractionOption option) {
        return option.getOptions().stream()
                .filter(subOption -> subOption.getValue().isPresent())
                .findFirst() // Select the first valid sub-option
                .map(subOption -> {
                    String value = subOption.getValue().get().asString();
                    String extracted = repostService.extractVideoIdSync(value);

                    return repostService.getByIdAndGuild(extracted, guildId);
                    // .doOnNext(ogMessage -> Mono.just(ogMessage));
                })
                .orElse(Mono.empty()); // In case there are no valid sub-options
    }
}
