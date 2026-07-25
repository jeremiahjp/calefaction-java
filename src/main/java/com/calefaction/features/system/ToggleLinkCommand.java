package com.calefaction.features.system;

import com.calefaction.config.BotProperties;
import com.calefaction.config.LinkFixerConfig;
import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
public class ToggleLinkCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final BotProperties botProperties;
    private final LinkFixerConfig linkFixerConfig;

    public ToggleLinkCommand(CommandRegistry commandRegistry, BotProperties botProperties, LinkFixerConfig linkFixerConfig) {
        this.commandRegistry = commandRegistry;
        this.botProperties = botProperties;
        this.linkFixerConfig = linkFixerConfig;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("toggle-link", "Admin command to enable or disable link fixing for specific domains")
                .addOptions(new OptionData(OptionType.STRING, "target", "The domain to toggle, or 'all'", true, true));
    }

    @Override
    public String getName() {
        return "toggle-link";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!botProperties.isAdmin(event.getUser().getId())) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        OptionMapping targetOption = event.getOption("target");
        if (targetOption == null) {
            event.reply("Please specify a target.").setEphemeral(true).queue();
            return;
        }

        String target = targetOption.getAsString();

        try {
            boolean isEnabled;
            if (target.equalsIgnoreCase("all")) {
                isEnabled = linkFixerConfig.toggleGlobal();
                String status = isEnabled ? "enabled" : "disabled";
                event.reply("Global link fixing is now " + status + ".").queue();
            } else {
                isEnabled = linkFixerConfig.toggleDomain(target);
                String status = isEnabled ? "enabled" : "disabled";
                event.reply("Link fixing for domain `" + target + "` is now " + status + ".").queue();
            }
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

        if (event.getFocusedOption().getName().equals("target")) {
            String input = event.getFocusedOption().getValue().toLowerCase();

            Stream<String> options = Stream.concat(
                    Stream.of("all"),
                    linkFixerConfig.getDomains().stream().map(LinkFixerConfig.DomainConfig::getPattern)
            );

            List<Command.Choice> choices = options
                    .filter(name -> name.toLowerCase().contains(input))
                    .limit(25)
                    .map(name -> new Command.Choice(name, name))
                    .collect(Collectors.toList());

            event.replyChoices(choices).queue();
        }
    }
}
