package com.calefaction.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Set<String> disabledCommands = ConcurrentHashMap.newKeySet();

    public void register(SlashCommand command) {
        commands.put(command.getName(), command);
        log.info("Registered command: {}", command.getName());
    }

    public boolean toggleCommand(String name) {
        if (!commands.containsKey(name)) {
            throw new IllegalArgumentException("Unknown command: " + name);
        }
        if (name.equals("toggle")) {
            throw new IllegalArgumentException("Cannot disable the toggle command.");
        }
        if (disabledCommands.contains(name)) {
            disabledCommands.remove(name);
            return true; // Command is now enabled
        } else {
            disabledCommands.add(name);
            return false; // Command is now disabled
        }
    }

    public Collection<String> getRegisteredCommandNames() {
        return commands.keySet();
    }

    public boolean isCommandDisabled(String name) {
        return disabledCommands.contains(name);
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
            if (isCommandDisabled(event.getName())) {
                event.reply("This command is currently disabled.").setEphemeral(true).queue();
            } else {
                command.execute(event);
            }
        } else {
            event.reply("Unknown command").setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        SlashCommand command = commands.get(event.getName());
        if (command != null && !isCommandDisabled(event.getName())) {
            command.onAutoComplete(event);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String commandName = componentId.split(":")[0];
        SlashCommand command = commands.get(commandName);
        if (command != null) {
            if (isCommandDisabled(commandName)) {
                event.reply("This command is currently disabled.").setEphemeral(true).queue();
            } else {
                command.onButton(event);
            }
        }
    }
}
