package com.jp.calefaction.commands;

import com.jp.calefaction.components.APICostCalculator;
import com.jp.calefaction.model.ai.ChatCompletionRequest;
import com.jp.calefaction.model.ai.ChatCompletionRequest.Message;
import com.jp.calefaction.service.ai.ChatGPTEmbesResponseService;
import com.jp.calefaction.service.ai.ChatGPTService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class ChatGPTCommand implements SlashCommand {

    private final ChatGPTService chatGPTService;
    private final ChatGPTEmbesResponseService embedResponseService;

    @Override
    public String getName() {
        return "chatgpt";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        Optional<String> queryOpt = event.getOption("query")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);
        log.info("inside the chatGPT handle query");
        if (queryOpt.isEmpty()) {
            return event.reply("You need to provide a query.").withEphemeral(true);
        }

        final String query = queryOpt.get();

        Message message = new Message();
        message.setContent(query);
        message.setRole("assistant");
        ChatCompletionRequest request = new ChatCompletionRequest("gpt-4", List.of(message));

        // Immediately reply with "Processing..."
        return event.reply("Processing...")
                .then(event.getInteractionResponse().getInitialResponse())
                .flatMap(originalResponse -> {
                    return chatGPTService.getChatCompletion(request).flatMap(response -> {
                        String cost = APICostCalculator.getFormattedCost(response);
                        String stringReply =
                                response.getChoices().get(0).getMessage().getContent();
                        log.info("Updating message");
                        return event.editReply("Processed.")
                                .withEmbeds(embedResponseService.createChatGPTEmbed(
                                        query, response.getChoices().get(0), cost));
                    });
                })
                .then();
    }
}
