package com.calefaction.features.games.megaslots;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MegaSlotsRTPTest {

    private static final Logger log = LoggerFactory.getLogger(MegaSlotsRTPTest.class);

    @Test
    void simulateRTP() {
        Random rng = new Random(42);
        long bet = 100L;
        int totalSpins = 100_000;

        long totalBet = 0;
        long totalWon = 0;
        int hits = 0;
        int bonusTriggers = 0;
        long totalBonusWon = 0;

        for (int i = 0; i < totalSpins; i++) {
            totalBet += bet;

            VideoSlotGrid grid = VideoSlotEngine.generateGrid(rng, false);
            var eval = VideoSlotEngine.evaluateSpin(grid, bet);

            long spinWin = eval.totalWin();
            if (spinWin > 0) {
                hits++;
            }

            // If bonus triggered, simulate the 10 free spins
            if (eval.triggeredBonus()) {
                bonusTriggers++;
                int persistentMultiplier = 1;
                long bonusWin = 0;

                for (int fs = 1; fs <= 10; fs++) {
                    VideoSlotGrid fsGrid = VideoSlotEngine.generateGrid(rng, false);
                    var fsEval = VideoSlotEngine.evaluateSpin(fsGrid, bet);

                    if (fsEval.multiplier() > 1) {
                        persistentMultiplier += fsEval.multiplier();
                    }

                    long fsSpinWin = fsEval.baseWin() * (long) persistentMultiplier;
                    bonusWin += fsSpinWin;
                }
                totalBonusWon += bonusWin;
                spinWin += bonusWin;
            }

            totalWon += spinWin;
        }

        double rtp = ((double) totalWon / totalBet) * 100.0;
        double hitRate = ((double) hits / totalSpins) * 100.0;
        double bonusRate = ((double) bonusTriggers / totalSpins) * 100.0;

        System.out.printf("=== MEGASLOTS RTP SIMULATION (%d spins) ===%n", totalSpins);
        System.out.printf("Total Bet: %,d coins%n", totalBet);
        System.out.printf("Total Won: %,d coins%n", totalWon);
        System.out.printf("Base + Bonus RTP: %.2f%%%n", rtp);
        System.out.printf("Hit Frequency: %.2f%%%n", hitRate);
        System.out.printf("Bonus Triggers: %d (1 in %.1f spins / %.2f%%)%n", bonusTriggers, (double) totalSpins / bonusTriggers, bonusRate);
        System.out.printf("Total Bonus Payout: %,d coins (avg %.1fx bet per bonus)%n", totalBonusWon, (double) totalBonusWon / (bonusTriggers * bet));

        org.junit.jupiter.api.Assertions.assertTrue(rtp >= 93.0 && rtp <= 99.5,
                "RTP should be between 93.0% and 99.5%, but was: " + rtp + "%");
        org.junit.jupiter.api.Assertions.assertTrue(hitRate >= 35.0 && hitRate <= 45.0,
                "Hit frequency should be between 35% and 45%, but was: " + hitRate + "%");
        org.junit.jupiter.api.Assertions.assertTrue(bonusTriggers > 0,
                "Bonus feature should trigger during 100k spins");
    }
}
