package com.jp.calefaction.service.ai;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.springframework.stereotype.Service;

@Service
public class ChatGPTEmbesResponseService {
    private static final int MAX_VALUE_LENGTH = 1024;
    private static final String CUTOFF_MESSAGE = "\n...\nThe rest was removed because Discord imposes 1024 char limit";

    public EmbedCreateSpec createChatGPTEmbed(String query, String response, String cost) {
        String message = response;
        if (isStringTooLong(response)) {
            message = message.substring(0, MAX_VALUE_LENGTH - CUTOFF_MESSAGE.length());
            message = message.concat(CUTOFF_MESSAGE);
        }

        return EmbedCreateSpec.builder()
                .color(Color.ORANGE)
                .title("ChatGPT - Work In Progress")
                .addField("Query", query, false)
                .addField("Cost", cost, false)
                .addField("Response", message, true)
                .footer("ChatGPT gpt-4", "")
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
                .addField("Query", query, false)
                .addField("Cost", cost, false)
                .addField("Response", message, true)
                .footer("ChatGPT gpt-4", "")
                .build();
    }
}
