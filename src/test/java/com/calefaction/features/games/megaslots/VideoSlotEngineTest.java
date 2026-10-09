package com.calefaction.features.games.megaslots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class VideoSlotEngineTest {

    @Test
    void testGridGenerationAndRender() {
        Random rng = new Random(123);
        VideoSlotGrid grid = VideoSlotEngine.generateGrid(rng, false);

        assertNotNull(grid);
        String rendered = grid.render();
        assertTrue(rendered.contains("╔═══════════════════════════╗"));
        assertTrue(rendered.contains("╚═══════════════════════════╝"));
    }

    @Test
    void testForcedBonusGeneratesAtLeastThreeScatters() {
        Random rng = new Random(456);
        VideoSlotGrid grid = VideoSlotEngine.generateGrid(rng, true);

        var eval = VideoSlotEngine.evaluateSpin(grid, 100);
        assertTrue(eval.scatterCount() >= 3);
        assertTrue(eval.triggeredBonus());
    }

    @Test
    void testExactWinningWaysCalculation() {
        // Construct a deterministic grid where Crowns appear on Reels 0, 1, 2
        VideoSlotSymbol[][] symbols = new VideoSlotSymbol[5][3];
        int[][] bombs = new int[5][3];

        // Fill all with Cherries initially
        for (int c = 0; c < 5; c++) {
            for (int r = 0; r < 3; r++) {
                symbols[c][r] = VideoSlotSymbol.CHERRY;
            }
        }

        // Put 1 Crown on reel 0, 2 Crowns on reel 1, 1 Crown on reel 2
        symbols[0][0] = VideoSlotSymbol.CROWN;
        symbols[1][0] = VideoSlotSymbol.CROWN;
        symbols[1][1] = VideoSlotSymbol.CROWN;
        symbols[2][0] = VideoSlotSymbol.CROWN;
        symbols[3][0] = VideoSlotSymbol.LEMON; // Breaks streak on reel 3

        VideoSlotGrid grid = new VideoSlotGrid(symbols, bombs);
        var eval = VideoSlotEngine.evaluateSpin(grid, 100L);

        // Should find Crowns winning with 3 match count and 1x2x1 = 2 ways!
        var crownWay = eval.winningWays().stream()
                .filter(w -> w.symbol() == VideoSlotSymbol.CROWN)
                .findFirst();

        assertTrue(crownWay.isPresent());
        assertEquals(3, crownWay.get().matchCount());
        assertEquals(2, crownWay.get().waysCount());
        assertTrue(eval.totalWin() > 0);
    }

    @Test
    void testMultiplierBombBoostsTotalWin() {
        VideoSlotSymbol[][] symbols = new VideoSlotSymbol[5][3];
        int[][] bombs = new int[5][3];

        for (int c = 0; c < 5; c++) {
            for (int r = 0; r < 3; r++) {
                symbols[c][r] = VideoSlotSymbol.LEMON;
            }
        }

        // Put a 5x multiplier bomb on reel 4
        symbols[4][0] = VideoSlotSymbol.MULTIPLIER;
        bombs[4][0] = 5;

        VideoSlotGrid grid = new VideoSlotGrid(symbols, bombs);
        var eval = VideoSlotEngine.evaluateSpin(grid, 100L);

        assertEquals(5, eval.multiplier());
        assertEquals(eval.baseWin() * 5, eval.totalWin());
    }
}
