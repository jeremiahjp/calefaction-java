package com.calefaction.features.games;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class SlotsCommandTest {

    private final Random random = new Random(42);

    @Test
    void testJackpotOutcome() {
        // outcome < 0.015 triggers a jackpot of 3 matching standard symbols
        SlotsCommand.SpinOutcome outcome = SlotsCommand.determineOutcome(0.005, random);

        assertEquals(outcome.s1(), outcome.s2());
        assertEquals(outcome.s2(), outcome.s3());
        assertNotEquals("🆓", outcome.s1());
        assertFalse(outcome.triggeredFreeSpins());
    }

    @Test
    void testFreeSpinsOutcome() {
        // 0.015 <= outcome < 0.050 triggers 3 free spins scatters
        SlotsCommand.SpinOutcome outcome = SlotsCommand.determineOutcome(0.030, random);

        assertEquals("🆓", outcome.s1());
        assertEquals("🆓", outcome.s2());
        assertEquals("🆓", outcome.s3());
        assertTrue(outcome.triggeredFreeSpins());
    }

    @Test
    void testSmallWinOutcome() {
        // 0.050 <= outcome < 0.280 triggers 2 matching symbols
        SlotsCommand.SpinOutcome outcome = SlotsCommand.determineOutcome(0.150, random);

        boolean match12 = outcome.s1().equals(outcome.s2());
        boolean match23 = outcome.s2().equals(outcome.s3());
        boolean match13 = outcome.s1().equals(outcome.s3());

        // Exactly one pair matches (small win)
        assertTrue(match12 || match23 || match13);
        assertFalse(match12 && match23); // Not a jackpot
        assertFalse(outcome.triggeredFreeSpins());
    }

    @Test
    void testLossOutcome() {
        // outcome >= 0.280 triggers 3 distinct symbols (no match)
        SlotsCommand.SpinOutcome outcome = SlotsCommand.determineOutcome(0.500, random);

        assertNotEquals(outcome.s1(), outcome.s2());
        assertNotEquals(outcome.s2(), outcome.s3());
        assertNotEquals(outcome.s1(), outcome.s3());
        assertFalse(outcome.triggeredFreeSpins());
    }

    @Test
    void testMonteCarloFairnessDistribution() {
        int jackpots = 0;
        int freeSpins = 0;
        int smallWins = 0;
        int losses = 0;
        int totalSpins = 50_000;

        Random rng = new Random(12345);
        for (int i = 0; i < totalSpins; i++) {
            double roll = rng.nextDouble();
            SlotsCommand.SpinOutcome outcome = SlotsCommand.determineOutcome(roll, rng);

            if (outcome.triggeredFreeSpins()) {
                freeSpins++;
            } else if (outcome.s1().equals(outcome.s2()) && outcome.s2().equals(outcome.s3())) {
                jackpots++;
            } else if (outcome.s1().equals(outcome.s2()) || outcome.s2().equals(outcome.s3()) || outcome.s1().equals(outcome.s3())) {
                smallWins++;
            } else {
                losses++;
            }
        }

        double jackpotRate = (double) jackpots / totalSpins;
        double freeSpinsRate = (double) freeSpins / totalSpins;
        double smallWinRate = (double) smallWins / totalSpins;
        double lossRate = (double) losses / totalSpins;

        // Expect ~1.5% jackpots (tolerance +/- 0.5%)
        assertEquals(0.015, jackpotRate, 0.005, "Jackpot rate should be ~1.5%");

        // Expect ~3.5% free spins (tolerance +/- 0.5%)
        assertEquals(0.035, freeSpinsRate, 0.005, "Free spins rate should be ~3.5%");

        // Expect ~23% small wins (tolerance +/- 1.0%)
        assertEquals(0.230, smallWinRate, 0.010, "Small win rate should be ~23.0%");

        // Expect ~72% losses (tolerance +/- 1.0%)
        assertEquals(0.720, lossRate, 0.010, "Loss rate should be ~72.0%");

        // Total hit rate is ~28%
        assertEquals(0.280, jackpotRate + freeSpinsRate + smallWinRate, 0.010, "Total win hit rate should be ~28.0%");
    }
}
