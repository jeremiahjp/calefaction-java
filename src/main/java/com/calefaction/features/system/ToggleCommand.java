package com.calefaction.features.system;

import com.calefaction.config.BotProperties;
import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

@Component
public class ToggleCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final BotProperties botProperties;

    public ToggleCommand(CommandRegistry commandRegistry, BotProperties botProperties) {
        this.commandRegistry = commandRegistry;
        this.botProperties = botProperties;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("toggle", "Admin command to enable or disable a command")
                .addOptions(new OptionData(OptionType.STRING, "command", "The command to toggle", true, true));
    }

    @Override
    public String getName() {
        return "toggle";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!botProperties.isAdmin(event.getUser().getId())) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        OptionMapping commandOption = event.getOption("command");
        if (commandOption == null) {
            event.reply("Please specify a command.").setEphemeral(true).queue();
            return;
        }

        String targetCommand = commandOption.getAsString();

        try {
            boolean isEnabled = commandRegistry.toggleCommand(targetCommand);
            String status = isEnabled ? "enabled" : "disabled";
            event.reply("Command `/" + targetCommand + "` is now " + status + ".").queue();
        } catch (IllegalArgumentException e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if (!botProperties.isAdmin(event.getUser().getId())) {
            event.replyChoices(List.of()).queue();
            return;
        }

        if (event.getFocusedOption().getName().equals("command")) {
            String input = event.getFocusedOption().getValue().toLowerCase();
            Collection<String> allCommands = commandRegistry.getRegisteredCommandNames();

            List<Command.Choice> choices = allCommands.stream()
                    .filter(name -> name.toLowerCase().contains(input) && !name.equals("toggle"))
                    .limit(25)
                    .map(name -> new Command.Choice(name, name))
                    .collect(Collectors.toList());

            event.replyChoices(choices).queue();
        }
    }
}
