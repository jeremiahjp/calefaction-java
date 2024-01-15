package com.jp.calefaction.service;

import com.jp.calefaction.dto.repost.TopRepostersDTO;
import com.jp.calefaction.entity.OriginalMessages;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import java.util.List;
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

    public EmbedCreateSpec createTopRepostEmbed(List<TopRepostersDTO> topReposters) {
        String formattedReposters = formatTopReposters(topReposters);
        return EmbedCreateSpec.builder()
                .color(Color.BLUE)
                .title("Top 5 Reposters")
                .description("These are the top 5 naughty reposters")
                .addField("Top Reposters", formattedReposters, false)
                // .addField("This was posted by ", "<@" + originalMessage.getSnowflakeId() + ">", false)
                // .addField(
                //         "Link",
                //         "https://discord.com/channels/" + originalMessage.getGuildId() + "/"
                //                 + originalMessage.getChannelId() + "/" + originalMessage.getMessageId(),
                //         false)
                .thumbnail("")
                // .footer("Repost Sniffer", "")
                .build();
    }

    public String formatTopReposters(List<TopRepostersDTO> topReposters) {
        StringBuilder builder = new StringBuilder();

        int rank = 1;
        for (TopRepostersDTO reposter : topReposters) {
            builder.append(rank++)
                    .append(". ")
                    .append(reposter.getSnowflakeId())
                    .append(" - ")
                    .append(reposter.getRepostCount())
                    .append("\n");
        }

        return builder.toString();
    }
}
