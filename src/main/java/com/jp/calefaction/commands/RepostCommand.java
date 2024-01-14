package com.jp.calefaction.commands;

import com.jp.calefaction.entity.OriginalMessages;
import com.jp.calefaction.service.RepostEmbedService;
import com.jp.calefaction.service.RepostService;
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
public class RepostCommand implements SlashCommand {

    private final RepostService repostService;
    private final RepostEmbedService repostEmbedService;

    @Override
    public String getName() {
        return "repost";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Optional<String> top = event.getOption("top")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        Optional<String> check = event.getOption("check")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        // command: /repost -top value -check value
        if (top.isPresent() && check.isPresent()) {
            return event.reply("top: " + top + " check: " + check).withEphemeral(true);
        }
        // command: /repost -top value
        if (top.isPresent() && !check.isPresent()) {
            return event.reply("top: " + top + " check: not present").withEphemeral(true);
        }

        // command: /repost -check value
        if (!top.isPresent() && check.isPresent()) {
            String extracted = repostService.extractVideoId(check.get());
            log.info("Checking for repost with the value {}", extracted);
            OriginalMessages message = repostService.getByIdAndGuild(
                    extracted, event.getInteraction().getGuildId().get().asString());
            if (message == null) {
                return event.reply("nobody has posted this yet").withEphemeral(true);
            }
            if (message.getGuildId()
                    .equals(event.getInteraction().getGuildId().get().asString())) {
                return event.reply()
                        .withEmbeds((repostEmbedService.createRepostEmbed(message)))
                        .withEphemeral(true);
            }
            return event.reply("Oops, something happened").withEphemeral(true);
        }
        return event.reply("neither were specified").withEphemeral(true);
    }
}
