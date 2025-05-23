package com.jp.calefaction;

import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GlobalCommandRegistrar implements ApplicationRunner {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestClient client;

    @Value("${chatGPT.version}")
    private String gptVersion;

    @Value("${grok.model}")
    private String grokModel;

    // Use the rest client provided by our Bean
    public GlobalCommandRegistrar(RestClient client) {
        this.client = client;
    }

    // This method will run only once on each start up and is automatically called with Spring
    @Override
    public void run(ApplicationArguments args) {
        // Convenience variables for the sake of easier to read code below.
        final ApplicationService applicationService = client.getApplicationService();
        final long applicationId = client.getApplicationId().block();

        // Create commands programmatically
        List<ApplicationCommandRequest> commands = new ArrayList<>();

        // Add all commands
        commands.add(createChatCommand());
        commands.add(createPingCommand());
        commands.add(createGreetCommand());
        commands.add(createCatCommand());
        commands.add(createDogCommand());
        commands.add(createTextgenCommand());
        commands.add(createUrbanDictionaryCommand());
        commands.add(createWeatherCommand());
        commands.add(createRepostCommand());

        // Bulk overwrite commands
        applicationService
                .bulkOverwriteGlobalApplicationCommand(applicationId, commands)
                .doOnNext(ignore -> LOGGER.debug("Successfully registered Global Commands"))
                .doOnError(e -> LOGGER.error("Failed to register global commands", e))
                .subscribe();
    }

    private ApplicationCommandRequest createChatCommand() {
        // Create model choices from configuration
        List<ApplicationCommandOptionChoiceData> modelChoices = new ArrayList<>();

        // Add GPT model from config
        modelChoices.add(ApplicationCommandOptionChoiceData.builder()
                .name(gptVersion)
                .value(gptVersion)
                .build());

        // Add Grok model from config
        modelChoices.add(ApplicationCommandOptionChoiceData.builder()
                .name(grokModel)
                .value(grokModel)
                .build());

        // Create response type choices
        List<ApplicationCommandOptionChoiceData> responseTypeChoices = List.of(
                ApplicationCommandOptionChoiceData.builder()
                        .name("Text")
                        .value("text")
                        .build(),
                ApplicationCommandOptionChoiceData.builder()
                        .name("Embed")
                        .value("embed")
                        .build());

        return ApplicationCommandRequest.builder()
                .name("chat")
                .description("Chat with an LLM")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("model")
                        .description("Which LLM to use")
                        .type(3) // STRING
                        .required(true)
                        .choices(modelChoices)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("query")
                        .description("Your query")
                        .type(3) // STRING
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("response_type")
                        .description("How to format the response")
                        .type(3) // STRING
                        .required(false)
                        .choices(responseTypeChoices)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("private")
                        .description("Only respond to you")
                        .type(5) // BOOLEAN
                        .required(false)
                        .build())
                .build();
    }

    private ApplicationCommandRequest createPingCommand() {
        return ApplicationCommandRequest.builder()
                .name("ping")
                .description("Ping pong!")
                .build();
    }

    private ApplicationCommandRequest createGreetCommand() {
        return ApplicationCommandRequest.builder()
                .name("greet")
                .description("Greets you")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Your name")
                        .type(3) // STRING
                        .required(true)
                        .build())
                .build();
    }

    private ApplicationCommandRequest createCatCommand() {
        List<ApplicationCommandOptionChoiceData> typeChoices = new ArrayList<>();
        typeChoices.add(ApplicationCommandOptionChoiceData.builder()
                .name("gif")
                .value("gif")
                .build());

        return ApplicationCommandRequest.builder()
                .name("cat")
                .description("Random cat image for you")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("type")
                        .description("Type of image")
                        .type(3) // STRING
                        .required(false)
                        .choices(typeChoices)
                        .build())
                .build();
    }

    private ApplicationCommandRequest createDogCommand() {
        List<ApplicationCommandOptionChoiceData> typeChoices = new ArrayList<>();
        typeChoices.add(ApplicationCommandOptionChoiceData.builder()
                .name("gif")
                .value("gif")
                .build());

        return ApplicationCommandRequest.builder()
                .name("dog")
                .description("Random dog image for you")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("type")
                        .description("Type of image")
                        .type(3) // STRING
                        .required(false)
                        .choices(typeChoices)
                        .build())
                .build();
    }

    private ApplicationCommandRequest createTextgenCommand() {
        return ApplicationCommandRequest.builder()
                .name("textgen")
                .description("Generate text")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("type")
                        .description("text")
                        .type(3) // STRING
                        .required(true)
                        .build())
                .build();
    }

    private ApplicationCommandRequest createUrbanDictionaryCommand() {
        return ApplicationCommandRequest.builder()
                .name("urbandictionary")
                .description("Search urban dictionary for a word")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("word")
                        .description("Word")
                        .type(3) // STRING
                        .required(true)
                        .build())
                .build();
    }

    private ApplicationCommandRequest createWeatherCommand() {
        List<ApplicationCommandOptionChoiceData> unitChoices = new ArrayList<>();
        unitChoices.add(ApplicationCommandOptionChoiceData.builder()
                .name("imperial")
                .value("imperial")
                .build());
        unitChoices.add(ApplicationCommandOptionChoiceData.builder()
                .name("metric")
                .value("metric")
                .build());
        unitChoices.add(ApplicationCommandOptionChoiceData.builder()
                .name("international")
                .value("international")
                .build());

        return ApplicationCommandRequest.builder()
                .name("weather")
                .description("Gets the Weather (Work in progress)")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("location")
                        .description("weather")
                        .type(3) // STRING
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("units")
                        .description("units")
                        .type(3) // STRING
                        .required(true)
                        .choices(unitChoices)
                        .build())
                .build();
    }

    private ApplicationCommandRequest createRepostCommand() {
        // Create the top subcommand
        ApplicationCommandOptionData topSubcommand = ApplicationCommandOptionData.builder()
                .name("top")
                .description("View top reposts")
                .type(1) // SUB_COMMAND
                .addOption(ApplicationCommandOptionData.builder()
                        .name("category")
                        .description("Select a category")
                        .type(3) // STRING
                        .required(false)
                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name("YouTube")
                                .value("YT")
                                .build())
                        .build())
                .build();

        // Create the check subcommand
        ApplicationCommandOptionData checkSubcommand = ApplicationCommandOptionData.builder()
                .name("check")
                .description("Check if a URL has been reposted")
                .type(1) // SUB_COMMAND
                .addOption(ApplicationCommandOptionData.builder()
                        .name("url")
                        .description("The URL to check")
                        .type(3) // STRING
                        .required(true)
                        .build())
                .build();

        return ApplicationCommandRequest.builder()
                .name("repost")
                .description("Repost commands")
                .addOption(topSubcommand)
                .addOption(checkSubcommand)
                .build();
    }
}
