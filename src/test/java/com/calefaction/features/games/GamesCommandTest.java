package com.calefaction.features.games;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.junit.jupiter.api.Test;

class GamesCommandTest {

    @Test
    void testCommandData() {
        GamesCommand gamesCommand = new GamesCommand(null, null, null, null, null);
        SlashCommandData data = (SlashCommandData) gamesCommand.getCommandData();

        assertEquals("games", data.getName());
        assertEquals("games", gamesCommand.getName());
        assertEquals(1, data.getOptions().size());
        assertEquals("game", data.getOptions().get(0).getName());
        assertTrue(data.getOptions().get(0).isAutoComplete());
        assertFalse(data.getOptions().get(0).isRequired());
    }

    @Test
    void testBuildGamesHubEmbedAllAvailable() {
        MessageEmbed embed = GamesCommand.buildGamesHubEmbed(false, false, false, 5000L);

        assertNotNull(embed);
        assertTrue(embed.getTitle().contains("Calefaction Games Arcade"));
        assertTrue(embed.getDescription().contains("5,000 🪙"));
        assertEquals(3, embed.getFields().size());

        MessageEmbed.Field contextoField = embed.getFields().get(0);
        assertTrue(contextoField.getName().contains("Contexto"));
        assertTrue(contextoField.getValue().contains("🟢 **Available**"));

        MessageEmbed.Field megaSlotsField = embed.getFields().get(1);
        assertTrue(megaSlotsField.getName().contains("Megaslots"));
        assertTrue(megaSlotsField.getValue().contains("🟢 **Available**"));

        MessageEmbed.Field slotsField = embed.getFields().get(2);
        assertTrue(slotsField.getName().contains("Super Slots"));
        assertTrue(slotsField.getValue().contains("🟢 **Available**"));
    }

    @Test
    void testBuildGamesHubEmbedDisabledState() {
        MessageEmbed embed = GamesCommand.buildGamesHubEmbed(true, false, true, 1000L);

        assertNotNull(embed);
        MessageEmbed.Field contextoField = embed.getFields().get(0);
        assertTrue(contextoField.getValue().contains("🔴 **Disabled**"));

        MessageEmbed.Field megaSlotsField = embed.getFields().get(1);
        assertTrue(megaSlotsField.getValue().contains("🔴 **Disabled**"));

        MessageEmbed.Field slotsField = embed.getFields().get(2);
        assertTrue(slotsField.getValue().contains("🟢 **Available**"));
    }

    @Test
    void testBuildActionRow() {
        ActionRow row = GamesCommand.buildActionRow(false, false, false);
        assertNotNull(row);
        assertEquals(3, row.getButtons().size());

        Button contextoBtn = row.getButtons().get(0);
        assertEquals("games:play:contexto", contextoBtn.getCustomId());
        assertFalse(contextoBtn.isDisabled());

        Button megaSlotsBtn = row.getButtons().get(1);
        assertEquals("games:play:megaslots", megaSlotsBtn.getCustomId());
        assertFalse(megaSlotsBtn.isDisabled());

        Button slotsBtn = row.getButtons().get(2);
        assertEquals("games:play:slots", slotsBtn.getCustomId());
        assertFalse(slotsBtn.isDisabled());

        // When disabled
        ActionRow disabledRow = GamesCommand.buildActionRow(true, true, true);
        assertTrue(disabledRow.getButtons().get(0).isDisabled());
        assertTrue(disabledRow.getButtons().get(1).isDisabled());
        assertTrue(disabledRow.getButtons().get(2).isDisabled());
    }
}
