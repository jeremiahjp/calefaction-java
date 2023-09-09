package com.jp.calefaction.service;

import com.jp.calefaction.entity.OriginalMessages;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.springframework.stereotype.Service;

@Service
public class RepostEmbedService {

    public EmbedCreateSpec createRepostEmbed(OriginalMessages originalMessage) {
        return EmbedCreateSpec.builder()
                .color(Color.BLUE)
                .title("Reposter!")
                .description("You naughty reposter")
                .addField("This was posted by ", "<@" + originalMessage.getSnowflakeId() + ">", false)
                .addField(
                        "Link",
                        "https://discord.com/channels/" + originalMessage.getGuildId() + "/"
                                + originalMessage.getChannelId() + "/" + originalMessage.getMessageId(),
                        false)
                .thumbnail("")
                // .footer("Repost Sniffer", "")
                .build();
    }
}
