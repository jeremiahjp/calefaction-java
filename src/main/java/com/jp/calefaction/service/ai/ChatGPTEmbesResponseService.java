package com.jp.calefaction.service.ai;

import com.jp.calefaction.model.ai.ChatCompletionResponse.Choice;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatGPTEmbesResponseService {
    private static final int MAX_VALUE_LENGTH = 1025;
    private static final String CUTOFF_MESSAGE = "\n...\nThe rest was trunc because Discord imposes 1024 char limit";

    @Value("${chatGPT.version}")
    public String gptVersion;

    public EmbedCreateSpec createChatGPTEmbed(String query, Choice choice, String cost) {
        String message = choice.getMessage().getContent();

        return EmbedCreateSpec.builder()
                .color(Color.ORANGE)
                .title("ChatGPT - Work In Progress")
                .description("**query response**\n" + "```" + message + "```")
                .addField("Finish reason", choice.getFinish_reason(), false)
                .addField("Query", query, false)
                .addField("Cost", cost, false)
                // .addField("Response", message, true)
                .footer("OpenAI " + gptVersion, "")
                .build();
    }

    public EmbedCreateSpec embededImageResponse(String query, String imageUrl, Choice choice, String cost) {
        String message = choice.getMessage().getContent();

        return EmbedCreateSpec.builder()
                .color(Color.CINNABAR)
                .title("ChatGPT - Work In Progress")
                .description("**query response**\n" + "```" + message + "```")
                .addField("Finish reason", choice.getFinish_reason(), false)
                .addField("Query", query, false)
                .addField("Image url", imageUrl, false)
                .addField("Cost", cost, false)
                // .addField("Response", message, true)
                .footer("OpenAI " + gptVersion, "")
                .build();
    }

    private boolean isStringTooLong(String s) {
        return s.length() > MAX_VALUE_LENGTH - CUTOFF_MESSAGE.length();
    }

    public EmbedCreateSpec createChatGPTEmbedLanguageFilter(String query, String response, String cost) {
        String message = response;
        if (isStringTooLong(response)) {
            message = message.substring(0, MAX_VALUE_LENGTH - CUTOFF_MESSAGE.length());
            message = message.concat(CUTOFF_MESSAGE);
        }

        return EmbedCreateSpec.builder()
                .color(Color.ORANGE)
                .title("ChatGPT - Work In Progress")
                .addField("finish reason", query, false)
                .addField("Query", query, false)
                .addField("Cost", cost, false)
                .addField("Response", message, true)
                .footer("OpenAI " + gptVersion, "")
                .build();
    }

    // public EmbedCreateSpec createAudioEmbed() {
    //     return EmbedCreateSpec.builder()
    //         .color(Color.PINK)
    //         .title("audio tts")
    //         .url("attachment://")
    // }
}
