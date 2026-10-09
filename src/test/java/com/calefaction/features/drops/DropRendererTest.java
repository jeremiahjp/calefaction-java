package com.calefaction.features.drops;

import static org.junit.jupiter.api.Assertions.*;

import com.calefaction.features.drops.model.TweetEntry;
import java.time.Instant;
import java.util.List;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;

class DropRendererTest {

    @Test
    void testBuildDropEmbed() {
        TweetEntry tweet = new TweetEntry(
                "112233",
                "Drop_Notify",
                "Drop Notify",
                "https://pbs.twimg.com/avatar.jpg",
                "Pokemon 151 Booster Bundles in stock at Target! https://target.com/p/123",
                Instant.now(),
                List.of("https://target.com/p/123"),
                List.of("https://pbs.twimg.com/card.jpg")
        );

        MessageEmbed embed = DropRenderer.buildDropEmbed(tweet, "151");
        assertNotNull(embed);
        assertTrue(embed.getTitle().contains("POKÉMON DROP ALERT"));
        assertTrue(embed.getTitle().contains("151"));
        assertEquals("Drop Notify (@Drop_Notify)", embed.getAuthor().getName());
        assertEquals("https://pbs.twimg.com/card.jpg", embed.getImage().getUrl());
        assertEquals(DropRenderer.POKEMON_YELLOW, embed.getColor());
    }

    @Test
    void testBuildActionRows() {
        TweetEntry tweet = new TweetEntry(
                "112233",
                "PokemonDealsTCG",
                "Pokemon Deals",
                "https://pbs.twimg.com/avatar.jpg",
                "Check out Walmart and Best Buy links",
                Instant.now(),
                List.of("https://www.bestbuy.com/site/123", "https://walmart.com/ip/456"),
                List.of()
        );

        List<ActionRow> rows = DropRenderer.buildActionRows(tweet);
        assertEquals(1, rows.size());
        List<Button> buttons = rows.get(0).getButtons();
        assertEquals(3, buttons.size()); // 1 for X + 2 store links

        assertEquals("🐦 View on X", buttons.get(0).getLabel());
        assertEquals("https://x.com/PokemonDealsTCG/status/112233", buttons.get(0).getUrl());

        assertEquals("🛒 Best Buy", buttons.get(1).getLabel());
        assertEquals("🛒 Walmart", buttons.get(2).getLabel());
    }

    @Test
    void testBuildMentionString() {
        String mentions = DropRenderer.buildMentionString(List.of("94220323628523520", "12345"), List.of("67890"));
        assertEquals("<@94220323628523520> <@12345> <@&67890>", mentions);

        assertEquals("", DropRenderer.buildMentionString(List.of(), List.of()));
        assertEquals("", DropRenderer.buildMentionString(null, null));
    }
}
