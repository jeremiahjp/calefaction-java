package com.calefaction.features.imagine;

import com.calefaction.config.BotProperties;
import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

@Component
public class ImagineCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final GrokImageService grokImageService;
    private final OpenAiVideoService openAiVideoService;
    private final BotProperties botProperties;

    public ImagineCommand(CommandRegistry commandRegistry, GrokImageService grokImageService, OpenAiVideoService openAiVideoService, BotProperties botProperties) {
        this.commandRegistry = commandRegistry;
        this.grokImageService = grokImageService;
        this.openAiVideoService = openAiVideoService;
        this.botProperties = botProperties;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("imagine", "Generate an image or video using Grok AI")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM,
                        InteractionContextType.PRIVATE_CHANNEL)
                .addSubcommands(
                        new SubcommandData("image", "Generate an image")
                                .addOption(OptionType.STRING, "prompt", "Describe the image you want to generate",
                                        true),
                        new SubcommandData("video", "Generate an AI video")
                                .addOption(OptionType.STRING, "prompt", "Describe the video you want to generate",
                                        true)
                                .addOption(OptionType.STRING, "provider", "Video provider (grok or openai)", false)
                                .addOptions(new net.dv8tion.jda.api.interactions.commands.build.OptionData(OptionType.INTEGER, "duration", "Video length in seconds", false)
                                        .addChoice("5 seconds", 5)
                                        .addChoice("10 seconds", 10)
                                        .addChoice("15 seconds", 15)
                                        .addChoice("20 seconds (OpenAI only)", 20)));
    }

    @Override
    public String getName() {
        return "imagine";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping promptOption = event.getOption("prompt");
        if (promptOption == null) {
            event.reply("Error: Prompt is required.").setEphemeral(true).queue();
            return;
        }
        String prompt = promptOption.getAsString();
        String subcommand = event.getSubcommandName();

        event.deferReply().queue();

        if ("video".equals(subcommand)) {
            if (!botProperties.isAdmin(event.getUser().getId())) {
                event.getHook().sendMessage("You are not authorized to use the video generation command.").queue();
                return;
            }

            OptionMapping providerOption = event.getOption("provider");
            String provider = providerOption != null ? providerOption.getAsString().toLowerCase() : "grok";

            OptionMapping durationOption = event.getOption("duration");
            int requestedDuration = durationOption != null ? durationOption.getAsInt() : 5;

            if ("grok".equals(provider) && requestedDuration > 15) {
                requestedDuration = 15;
                event.getHook().sendMessage("⚠️ Grok video generation is limited to 15 seconds. Capping your request to 15s.").setEphemeral(true).queue();
            }
            final int finalDuration = requestedDuration;

            reactor.core.publisher.Mono<String> videoMono;
            if ("openai".equals(provider)) {
                videoMono = openAiVideoService.generateVideo(prompt, finalDuration, null);
            } else {
                videoMono = grokImageService.generateVideo(prompt, finalDuration, null);
            }

            videoMono
                    .switchIfEmpty(reactor.core.publisher.Mono.error(new RuntimeException("No result returned.")))
                    .flatMap(videoUrl -> {
                        if ("openai".equals(provider)) {
                            return openAiVideoService.downloadMedia(videoUrl);
                        } else {
                            return grokImageService.downloadMedia(videoUrl);
                        }
                    })
                    .switchIfEmpty(reactor.core.publisher.Mono.error(new RuntimeException("Failed to download video.")))
                    .subscribe(videoBytes -> {
                        String costStr = "openai".equals(provider) ? "Sora API" : "Cost: $0.24";
                        event.getHook().sendFiles(FileUpload.fromData(videoBytes, "video.mp4"))
                                .setContent("Here is your generated video for: **" + prompt + "**\n\n"
                                        + "Powered by " + provider.toUpperCase() + " • " + costStr)
                                .queue();
                    }, error -> {
                        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException ex) {
                            if (ex.getStatusCode().value() == 429) {
                                event.getHook().sendMessage("Insert a quarter to continue").queue();
                                return;
                            }
                        }
                        event.getHook().sendMessage("Failed to generate video: " + error.getMessage()).queue();
                    });
        } else {
            // Default to image
            grokImageService.generateImage(prompt)
                    .switchIfEmpty(reactor.core.publisher.Mono.error(new RuntimeException("No result returned.")))
                    .flatMap(imageUrl -> grokImageService.downloadMedia(imageUrl))
                    .switchIfEmpty(reactor.core.publisher.Mono.error(new RuntimeException("Failed to download image.")))
                    .subscribe(imageBytes -> {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Generated Image");
                        eb.setDescription("**Prompt:** " + prompt);
                        eb.setImage("attachment://image.png");
                        eb.setColor(new Color(0, 0, 0));
                        eb.setFooter("Powered by Grok AI • Cost: $0.07");

                        event.getHook().sendFiles(FileUpload.fromData(imageBytes, "image.png"))
                                .setEmbeds(eb.build())
                                .queue();
                    }, error -> {
                        event.getHook().sendMessage("Failed to generate image: " + error.getMessage()).queue();
                    });
        }
    }
}
