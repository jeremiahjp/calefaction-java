package com.calefaction.features.speak;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

@Component
public class SpeakCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final GrokTtsService grokTtsService;

    public SpeakCommand(CommandRegistry commandRegistry, GrokTtsService grokTtsService) {
        this.commandRegistry = commandRegistry;
        this.grokTtsService = grokTtsService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("speak", "Convert text to speech using Grok TTS")
                .addOptions(
                        new OptionData(OptionType.STRING, "text", "The text to convert to speech", true),
                        new OptionData(OptionType.STRING, "voice", "The voice to use", false)
                                .addChoices(
                                        new Choice("Eve", "eve"),
                                        new Choice("Ara", "ara"),
                                        new Choice("Leo", "leo"),
                                        new Choice("Rex", "rex"),
                                        new Choice("Sal", "sal")
                                )
                );
    }

    @Override
    public String getName() {
        return "speak";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping textOption = event.getOption("text");
        if (textOption == null) {
            event.reply("Text is required.").setEphemeral(true).queue();
            return;
        }

        String text = textOption.getAsString();
        String voice = event.getOption("voice") != null ? event.getOption("voice").getAsString() : "eve";

        event.deferReply().queue();

        grokTtsService.generateSpeech(text, voice).subscribe(
                audioBytes -> {
                    event.getHook().sendFiles(FileUpload.fromData(audioBytes, "speech.mp3"))
                            .setContent("🗣️ Here is your speech!")
                            .queue();
                },
                error -> {
                    event.getHook().sendMessage("Failed to generate speech: " + error.getMessage()).queue();
                }
        );
    }
}
