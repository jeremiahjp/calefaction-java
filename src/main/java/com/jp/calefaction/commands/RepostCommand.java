package com.jp.calefaction.commands;

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

    @Override
    public String getName() {
        return "repost";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        if (event.getInteraction().getUser().getId().asLong() == 94220323628523520L) {
            log.info(
                    "someone is probing this. user: {}",
                    event.getInteraction().getUser().getId().asString());
            return event.reply("work in progress.").withEphemeral(true);
        }

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
        if (top.isPresent() && !check.isPresent()) {
            return event.reply("top: " + top + " check: not present").withEphemeral(true);
        }

        // Just check top
        if (!top.isPresent() && check.isPresent()) {
            // List<String>
            return event.reply(repostService.getAllUsers().get(0).getSnowflakeId());
            // return event.reply("top is not present " + "check: " + check).withEphemeral(true);
        }
        return event.reply("neither were specified").withEphemeral(true);
    }
}
