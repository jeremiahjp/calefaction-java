package com.calefaction.features.chat;

import com.calefaction.config.LlmConfig;
import com.calefaction.core.CommandRegistry;
import com.calefaction.core.PaginationService;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.chat.model.ChatRequest;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatCommand implements SlashCommand {

    private static final Logger log = LoggerFactory.getLogger(ChatCommand.class);

    private final CommandRegistry commandRegistry;
    private final Map<String, LlmService> llmServices;
    private final LlmConfig llmConfig;
    private final PaginationService paginationService;

    @Value("${chat.message-chunk-size:500}")
    private int messageChunkSize;

    public ChatCommand(CommandRegistry commandRegistry, List<LlmService> services,
            LlmConfig llmConfig, PaginationService paginationService) {
        this.commandRegistry = commandRegistry;
        this.llmServices = services.stream()
                .collect(Collectors.toMap(LlmService::getProviderName, Function.identity()));
        this.llmConfig = llmConfig;
        this.paginationService = paginationService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("chat", "Chat with an LLM")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM,
                        InteractionContextType.PRIVATE_CHANNEL)
                .addOptions(
                        new OptionData(OptionType.STRING, "model", "The AI model to use", true)
                                .setAutoComplete(true),
                        new OptionData(OptionType.STRING, "query", "The prompt for the LLM", true),
                        new OptionData(OptionType.ATTACHMENT, "image", "Optional image to analyze", false));
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping queryOption = event.getOption("query");
        OptionMapping modelOption = event.getOption("model");
        if (queryOption == null || modelOption == null) {
            event.reply("Error: Required fields missing.").setEphemeral(true).queue();
            return;
        }
        String query = queryOption.getAsString();
        String modelId = modelOption.getAsString();

        log.info("[Chat] Command received - User: {}, Model: {}, Query length: {}",
                event.getUser().getName(), modelId, query.length());

        event.deferReply().queue();

        // Look up provider based on modelId
        String provider = llmConfig.getProviders().entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled() &&
                        entry.getValue().getModels().stream().anyMatch(mc -> mc.getId().equals(modelId)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (provider == null) {
            log.warn("[Chat] Invalid or disabled model selected: {}", modelId);
            event.getHook().sendMessage("Invalid or disabled model selected: " + modelId).queue();
            return;
        }

        log.info("[Chat] Using provider: {} for model: {}", provider, modelId);

        LlmService service = llmServices.get(provider);
        if (service == null) {
            event.getHook().sendMessage("Provider service not found: " + provider).queue();
            return;
        }

        String imageUrl = null;
        if (event.getOption("image") != null) {
            Message.Attachment attachment = event.getOption("image").getAsAttachment();
            if (attachment.isImage()) {
                imageUrl = attachment.getUrl();
                log.info("[Chat] Image attachment detected - URL: {}", imageUrl);
            }
        }
        final String finalImageUrl = imageUrl; // for lambda capture

        ChatRequest request = ChatRequest.builder()
                .prompt(query)
                .modelId(modelId)
                .imageUrl(finalImageUrl)
                .build();

        log.info("[Chat] Calling LLM service: {}", provider);
        service.generateResponse(request).subscribe(chatResponse -> {
            String content = chatResponse.getContent();
            log.info("[Chat] Response received - Content length: {}, Provider: {}",
                    content.length(), chatResponse.getProvider());

            // Cost calculation
            String costInfo = "";
            if (chatResponse.getUsage() != null) {
                LlmConfig.ModelChoice modelChoice = llmConfig.getProviders().values().stream()
                        .flatMap(p -> p.getModels().stream())
                        .filter(m -> m.getId().equals(modelId))
                        .findFirst()
                        .orElse(null);

                if (modelChoice != null) {
                    double inputCost = (chatResponse.getUsage().getPromptTokens() / 1_000_000.0)
                            * modelChoice.getInputPricePerMillion();
                    double outputCost = (chatResponse.getUsage().getCompletionTokens() / 1_000_000.0)
                            * modelChoice.getOutputPricePerMillion();
                    double totalCost = inputCost + outputCost;

                    costInfo = String.format(
                            "\n\n---\n**📊 Usage:** %d input + %d output = %d tokens\n**💰 Cost:** $%.6f",
                            chatResponse.getUsage().getPromptTokens(),
                            chatResponse.getUsage().getCompletionTokens(),
                            chatResponse.getUsage().getTotalTokens(),
                            totalCost);
                }
            }

            String fullResponse = content + costInfo;

            if (finalImageUrl != null && !finalImageUrl.isEmpty()) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setDescription(
                                fullResponse.length() > 4096 ? fullResponse.substring(0, 4093) + "..." : fullResponse)
                        .setImage(finalImageUrl)
                        .setFooter("Analyzed image");

                event.getHook().sendMessageEmbeds(embed.build()).queue();
            } else if (fullResponse.length() <= messageChunkSize) {
                event.getHook().sendMessage(fullResponse).queue();
            } else {
                List<String> chunks = paginationService.splitMessage(fullResponse, messageChunkSize);
                String id = UUID.randomUUID().toString();
                paginationService.store(id, chunks);

                event.getHook().sendMessage(chunks.get(0))
                        .setComponents(ActionRow.of(Button.primary("chat:next:0:" + id, "Next")))
                        .queue();
            }
        }, error -> {
            log.error("[Chat] Error processing request - Model: {}, Error: {}", modelId, error.getMessage(), error);
            event.getHook().sendMessage("Failed to get response: " + error.getMessage()).queue();
        });
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        String action = parts[1];
        int currentPage = Integer.parseInt(parts[2]);
        String id = parts[3];

        List<String> chunks = paginationService.get(id);
        if (chunks == null) {
            event.reply("Session expired.").setEphemeral(true).queue();
            return;
        }

        int newPage = action.equals("next") ? currentPage + 1 : currentPage - 1;
        if (newPage < 0 || newPage >= chunks.size()) {
            event.deferEdit().queue();
            return;
        }

        List<Button> buttons = new ArrayList<>();
        if (newPage > 0) {
            buttons.add(Button.secondary("chat:back:" + newPage + ":" + id, "Back"));
        }
        if (newPage < chunks.size() - 1) {
            buttons.add(Button.primary("chat:next:" + newPage + ":" + id, "Next"));
        }

        event.editMessage(chunks.get(newPage))
                .setComponents(buttons.isEmpty() ? Collections.emptyList()
                        : List.of(ActionRow.of(buttons)))
                .queue();
    }

    @Override
    public void onAutoComplete(
            CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("chat") && event.getFocusedOption().getName().equals("model")) {
            String typed = event.getFocusedOption().getValue().toLowerCase();

            List<Command.Choice> choices = llmConfig.getProviders()
                    .entrySet().stream()
                    .filter(entry -> entry.getValue().isEnabled())
                    .flatMap(entry -> entry.getValue().getModels().stream()
                            .map(mc -> new Command.Choice(
                                    mc.getName(), mc.getId())))
                    .filter(choice -> choice.getName().toLowerCase().contains(typed))
                    .limit(25)
                    .collect(Collectors.toList());

            event.replyChoices(choices).queue();
        }
    }
}
