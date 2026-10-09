package com.calefaction.features.drops;

import com.calefaction.features.drops.model.TweetEntry;
import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class DropRenderer {

    public static final Color POKEMON_YELLOW = new Color(0xFF, 0xCB, 0x05);
    public static final Color POKEBALL_RED = new Color(0xEE, 0x15, 0x15);

    public static MessageEmbed buildDropEmbed(TweetEntry tweet, String matchedKeyword) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(POKEMON_YELLOW);
        String title;
        if ("jjuddpi".equalsIgnoreCase(tweet.screenName()) || "TEST ALERT".equalsIgnoreCase(matchedKeyword)) {
            title = "🧪 TWITTER TEST ALERT";
        } else {
            title = "⚡ POKÉMON DROP ALERT";
            if (matchedKeyword != null && !matchedKeyword.isBlank() && !"POKÉMON TCG".equalsIgnoreCase(matchedKeyword)) {
                title += " (" + matchedKeyword.toUpperCase() + ")";
            }
        }
        eb.setTitle(title, tweet.getTweetUrl());

        eb.setAuthor(
                String.format("%s (@%s)", tweet.userName(), tweet.screenName()),
                tweet.getTweetUrl(),
                tweet.profileImageUrl()
        );

        eb.setDescription(tweet.text());

        if (tweet.mediaUrls() != null && !tweet.mediaUrls().isEmpty()) {
            eb.setImage(tweet.mediaUrls().get(0));
        }

        if (tweet.createdAt() != null) {
            eb.setTimestamp(tweet.createdAt());
        }

        eb.setFooter("Calefaction Drop Monitor • X/Twitter", null);
        return eb.build();
    }

    public static List<ActionRow> buildActionRows(TweetEntry tweet) {
        List<Button> buttons = new ArrayList<>();

        // View on X button
        buttons.add(Button.link(tweet.getTweetUrl(), "🐦 View on X"));

        // Add retailer / store link buttons from extracted URLs
        int linkCount = 1;
        if (tweet.urls() != null) {
            for (String url : tweet.urls()) {
                if (buttons.size() >= 5) {
                    break;
                }
                // Don't duplicate the tweet permalink itself
                if (url.contains("twitter.com/") || url.contains("x.com/")) {
                    continue;
                }
                String label = getStoreLabel(url, linkCount++);
                try {
                    buttons.add(Button.link(url, label));
                } catch (Exception ignored) {
                }
            }
        }

        return buttons.isEmpty() ? List.of() : List.of(ActionRow.of(buttons));
    }

    public static String buildMentionString(List<String> userIds, List<String> roleIds) {
        StringBuilder sb = new StringBuilder();
        if (userIds != null) {
            for (String uid : userIds) {
                if (!uid.isBlank()) {
                    sb.append("<@").append(uid.trim()).append("> ");
                }
            }
        }
        if (roleIds != null) {
            for (String rid : roleIds) {
                if (!rid.isBlank()) {
                    sb.append("<@&").append(rid.trim()).append("> ");
                }
            }
        }
        return sb.toString().trim();
    }

    private static String getStoreLabel(String url, int index) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null) {
                host = host.toLowerCase();
                if (host.contains("amazon.")) return "🛒 Amazon";
                if (host.contains("target.")) return "🛒 Target";
                if (host.contains("bestbuy.")) return "🛒 Best Buy";
                if (host.contains("walmart.")) return "🛒 Walmart";
                if (host.contains("pokemoncenter.")) return "🛒 Pokémon Center";
                if (host.contains("gamestop.")) return "🛒 GameStop";
                if (host.contains("costco.")) return "🛒 Costco";
                if (host.contains("samclub.")) return "🛒 Sam's Club";
                if (host.contains("tiktok.")) return "🛒 TikTok Shop";
            }
        } catch (Exception ignored) {
        }
        return "🛒 Store Link " + index;
    }
}
