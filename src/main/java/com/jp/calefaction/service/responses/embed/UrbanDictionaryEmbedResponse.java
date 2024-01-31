package com.jp.calefaction.service.responses.embed;

import com.jp.calefaction.model.urbandictionary.UrbanDictionaryResponse;
import com.jp.calefaction.model.urbandictionary.UrbanDictionaryResponse.Entry;
import com.jp.calefaction.service.urbandictionary.UrbanDictionarySorter;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.text.SimpleDateFormat;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UrbanDictionaryEmbedResponse implements EmbedResponseService<UrbanDictionaryResponse, EmbedCreateSpec> {

    private static final int MAX_LENGTH = 1024;
    private static final String CUTOFF_MESSAGE = "\n...\nThe rest was removed because Discord imposes 1024 char limit";

    @Override
    public EmbedCreateSpec createEmbedResponse(UrbanDictionaryResponse response) {

        UrbanDictionaryResponse sorted = UrbanDictionarySorter.sortEntriesByPopularity(response);

        Entry list1 = sorted.getList().get(0);
        log.info("Creating urban dictionary embed");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String definition = list1.getDefinition();

        if (isStringTooLong(definition)) {
            definition = definition.substring(0, MAX_LENGTH - CUTOFF_MESSAGE.length());
            definition = definition.concat(CUTOFF_MESSAGE);
        }

        return EmbedCreateSpec.builder()
                .color(randomColor())
                .title("Urban Dictionary")
                .description("__" + list1.getWord() + "__")
                .addField("Definition", definition, false)
                .addField("Example", list1.getExample(), false)
                .addField("Written on", dateFormat.format(list1.getWritten_on()), false)
                .addField("Author", list1.getAuthor(), false)
                .addField("Link", list1.getPermalink(), false)
                .addField("Upvotes", String.valueOf(list1.getThumbs_up()), false)
                .addField("Downvotes", String.valueOf(list1.getThumbs_down()), false)
                .footer("Urban Dictionary - Calefaction", "")
                .build();
    }

    private boolean isStringTooLong(String s) {
        return s.length() > MAX_LENGTH - CUTOFF_MESSAGE.length();
    }

    private Color randomColor() {
        Random random = new Random();
        return Color.of(random.nextInt(0xFFFFFF));
    }
}
