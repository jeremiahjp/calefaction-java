package com.calefaction.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CommandRegistry extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);
    private final Map<String, SlashCommand> commands = new HashMap<>();

    public void register(SlashCommand command) {
        commands.put(command.getName(), command);
        log.info("Registered command: {}", command.getName());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<CommandData> commandDataList = new ArrayList<>();
        for (SlashCommand command : commands.values()) {
            commandDataList.add(command.getCommandData());
        }
        event.getJDA().updateCommands().addCommands(commandDataList).queue(
                success -> log.info("Successfully registered {} commands with Discord", commandDataList.size()),
                error -> log.error("Failed to register commands", error));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        SlashCommand command = commands.get(event.getName());
        if (command != null) {
            command.execute(event);
        } else {
            event.reply("Unknown command").setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        SlashCommand command = commands.get(event.getName());
        if (command != null) {
            command.onAutoComplete(event);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String commandName = componentId.split(":")[0];
        SlashCommand command = commands.get(commandName);
        if (command != null) {
            command.onButton(event);
        }
    }
}
