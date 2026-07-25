package com.calefaction.features.urbandictionary;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

@Component
public class UrbanDictionaryCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final UrbanDictionaryService urbanDictionaryService;

    public UrbanDictionaryCommand(CommandRegistry commandRegistry, UrbanDictionaryService urbanDictionaryService) {
        this.commandRegistry = commandRegistry;
        this.urbanDictionaryService = urbanDictionaryService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("urbandictionary", "Get the definition of a word")
                .addOptions(
                        new OptionData(OptionType.STRING, "term", "The word to define", true));
    }

    @Override
    public String getName() {
        return "urbandictionary";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String term = event.getOption("term").getAsString();

        event.deferReply().queue();

        urbanDictionaryService.define(term).subscribe(def -> {
            // Remove brackets [] typically found in UD definitions
            String cleanDefinition = def.definition().replace("[", "").replace("]", "");
            String cleanExample = def.example().replace("[", "").replace("]", "");

            // Truncate if too long (Discord limits)
            if (cleanDefinition.length() > 1000)
                cleanDefinition = cleanDefinition.substring(0, 990) + "...";
            if (cleanExample.length() > 500)
                cleanExample = cleanExample.substring(0, 490) + "...";

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(def.word(), def.permalink());
            eb.setDescription(cleanDefinition);
            eb.addField("Example", cleanExample, false);
            eb.setFooter("By " + def.author() + " | \uD83D\uDC4D " + def.thumbsUp());
            eb.setColor(new Color(29, 36, 57)); // Urban Dictionary Blue-ish

            event.getHook().sendMessageEmbeds(eb.build()).queue();
        }, error -> {
            event.getHook().sendMessage("Could not fetch definition: " + error.getMessage()).queue();
        }, () -> {
            event.getHook().sendMessage("Could not find definition for: " + term).queue();
        });
    }
}
