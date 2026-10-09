package com.calefaction.features.games.contexto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.calefaction.features.games.contexto.model.ContextoGuess;
import com.calefaction.features.games.contexto.model.ContextoSession;
import java.time.Instant;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;

class ContextoRendererTest {

    @Test
    void testGetBadge() {
        assertEquals("🎉", ContextoRenderer.getBadge(0));
        assertEquals("🎉", ContextoRenderer.getBadge(1));
        assertEquals("🟢", ContextoRenderer.getBadge(10));
        assertEquals("🟢", ContextoRenderer.getBadge(300));
        assertEquals("🟡", ContextoRenderer.getBadge(301));
        assertEquals("🟡", ContextoRenderer.getBadge(1500));
        assertEquals("🔴", ContextoRenderer.getBadge(1501));
        assertEquals("🔴", ContextoRenderer.getBadge(10000));
    }

    @Test
    void testBuildBoardEmbed() {
        ContextoSession session = new ContextoSession("user-1", 1458, true);
        session.addGuess(new ContextoGuess("cat", 450, Instant.now(), false));
        session.addGuess(new ContextoGuess("dog", 120, Instant.now(), false));

        MessageEmbed embed = ContextoRenderer.buildBoardEmbed(session, null);
        assertNotNull(embed);
        assertTrue(embed.getTitle().contains("1458"));
        assertTrue(embed.getTitle().contains("Daily"));
        assertEquals(2, session.getTotalGuesses());
    }

    @Test
    void testBuildScorecard() {
        ContextoSession session = new ContextoSession("user-1", 1458, true);
        session.addGuess(new ContextoGuess("water", 2500, Instant.now(), false));
        session.addGuess(new ContextoGuess("shirt", 500, Instant.now(), false));
        session.addGuess(new ContextoGuess("hat", 10, Instant.now(), false));
        session.addGuess(new ContextoGuess("bandana", 0, Instant.now(), false));

        String scorecard = ContextoRenderer.buildScorecard(session, null);
        assertTrue(scorecard.contains("Contexto #1458"));
        assertTrue(scorecard.contains("4"));
        assertTrue(scorecard.contains("🟢 2"));
        assertTrue(scorecard.contains("🟡 1"));
        assertTrue(scorecard.contains("🔴 1"));
    }

    @Test
    void testBuildScorecardActionRows() {
        var rows = ContextoRenderer.buildScorecardActionRows();
        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getButtons().size());
        var btn = rows.get(0).getButtons().get(0);
        assertEquals("contexto:launch:shared", btn.getCustomId());
        assertEquals("🧩 Play Contexto", btn.getLabel());
    }

    @Test
    void testBuildGiveUpConfirmActionRows() {
        var rows = ContextoRenderer.buildGiveUpConfirmActionRows("user-1");
        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).getButtons().size());
        assertEquals("contexto:confirm_giveup:user-1", rows.get(0).getButtons().get(0).getCustomId());
        assertEquals("contexto:back:user-1", rows.get(0).getButtons().get(1).getCustomId());
    }

    @Test
    void testBuildGiveUpConfirmEmbed() {
        ContextoSession session = new ContextoSession("user-1", 1458, true);
        session.addGuess(new ContextoGuess("water", 50, Instant.now(), false));

        MessageEmbed embed = ContextoRenderer.buildGiveUpConfirmEmbed(session, null);
        assertNotNull(embed);
        assertTrue(embed.getTitle().contains("Give Up"));
        assertTrue(embed.getDescription().contains("Are you sure you want to give up"));
    }

    @Test
    void testBuildAdminPanelEmbed() {
        ContextoSession session = new ContextoSession("user-target", 1460, true);
        session.addGuess(new ContextoGuess("beach", 15, Instant.now(), false));
        ContextoLeaderboardService.LeaderboardEntry entry =
                new ContextoLeaderboardService.LeaderboardEntry("user-target", 15, 0, Instant.now());

        MessageEmbed embed = ContextoRenderer.buildAdminPanelEmbed(1460, null, "user-target", session, entry, "Status: OK");
        assertNotNull(embed);
        assertTrue(embed.getTitle().contains("1460"));
        assertTrue(embed.getTitle().contains("Admin Panel"));
        assertTrue(embed.getDescription().contains("user-target"));
        assertTrue(embed.getDescription().contains("Status: OK"));
    }

    @Test
    void testBuildAdminActionRows() {
        var rows = ContextoRenderer.buildAdminActionRows("user-target", false);
        assertEquals(2, rows.size());

        // Row 1: SelectMenu
        var menu = rows.get(0).getComponents().get(0).asEntitySelectMenu();
        assertNotNull(menu);
        assertEquals("contexto:admin:select_user", menu.getCustomId());

        // Row 2: Buttons
        var buttons = rows.get(1).getButtons();
        assertEquals(4, buttons.size());
        assertEquals("contexto:admin:reset:user-target", buttons.get(0).getCustomId());
        assertEquals("contexto:admin:inspect:user-target", buttons.get(1).getCustomId());
        assertEquals("contexto:admin:reset_self", buttons.get(2).getCustomId());
        assertEquals("contexto:admin:close", buttons.get(3).getCustomId());

        // Self mode: 3 buttons (no separate reset_self button)
        var selfRows = ContextoRenderer.buildAdminActionRows("user-target", true);
        assertEquals(3, selfRows.get(1).getButtons().size());
    }

    @Test
    void testBuildResetConfirmEmbed() {
        ContextoSession session = new ContextoSession("user-target", 1460, true);
        session.addGuess(new ContextoGuess("coffee", 25, Instant.now(), false));

        MessageEmbed embed = ContextoRenderer.buildResetConfirmEmbed(1460, null, "user-target", session, false);
        assertNotNull(embed);
        assertTrue(embed.getTitle().contains("Confirm Reset"));
        assertTrue(embed.getDescription().contains("user-target"));
        assertTrue(embed.getDescription().contains("irreversible"));
    }

    @Test
    void testBuildResetConfirmActionRows() {
        var rows = ContextoRenderer.buildResetConfirmActionRows("user-target", false);
        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).getButtons().size());

        var confirmBtn = rows.get(0).getButtons().get(0);
        var cancelBtn = rows.get(0).getButtons().get(1);

        assertEquals("contexto:admin:confirm_reset:user-target", confirmBtn.getCustomId());
        assertEquals("contexto:admin:cancel_reset:user-target", cancelBtn.getCustomId());
        assertEquals("⚠️ Yes, Reset Player", confirmBtn.getLabel());

        var selfRows = ContextoRenderer.buildResetConfirmActionRows("user-target", true);
        assertEquals("⚠️ Yes, Reset Myself", selfRows.get(0).getButtons().get(0).getLabel());
    }
}
