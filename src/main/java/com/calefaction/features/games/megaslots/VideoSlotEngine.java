package com.calefaction.features.games.megaslots;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VideoSlotEngine {

    private static final int[] BOMB_VALUES = {2, 3, 5, 10, 25, 50, 100};
    private static final int[] BOMB_WEIGHTS = {45, 25, 15, 10, 3, 1, 1};

    private static final List<VideoSlotSymbol> SYMBOL_POOL = new ArrayList<>();

    static {
        for (VideoSlotSymbol sym : VideoSlotSymbol.values()) {
            for (int i = 0; i < sym.getWeight(); i++) {
                SYMBOL_POOL.add(sym);
            }
        }
    }

    public record WinningWay(
            VideoSlotSymbol symbol,
            int matchCount,
            int waysCount,
            long payout
    ) {}

    public record SpinEvaluation(
            VideoSlotGrid grid,
            List<WinningWay> winningWays,
            long baseWin,
            int multiplier,
            long totalWin,
            int scatterCount,
            boolean triggeredBonus
    ) {}

    public static VideoSlotGrid generateGrid(Random rng, boolean forceBonus) {
        VideoSlotSymbol[][] symbols = new VideoSlotSymbol[VideoSlotGrid.REELS][VideoSlotGrid.ROWS];
        int[][] bombs = new int[VideoSlotGrid.REELS][VideoSlotGrid.ROWS];

        for (int reel = 0; reel < VideoSlotGrid.REELS; reel++) {
            for (int row = 0; row < VideoSlotGrid.ROWS; row++) {
                VideoSlotSymbol sym = SYMBOL_POOL.get(rng.nextInt(SYMBOL_POOL.size()));
                symbols[reel][row] = sym;
                if (sym == VideoSlotSymbol.MULTIPLIER) {
                    bombs[reel][row] = rollBombMultiplier(rng);
                }
            }
        }

        if (forceBonus) {
            // Guarantee at least 3 Scatters across distinct reels
            List<Integer> reels = new ArrayList<>(List.of(0, 1, 2, 3, 4));
            java.util.Collections.shuffle(reels, rng);
            int placed = 0;
            for (int reel : reels) {
                if (placed >= 3) break;
                boolean hasScatter = false;
                for (int row = 0; row < VideoSlotGrid.ROWS; row++) {
                    if (symbols[reel][row] == VideoSlotSymbol.SCATTER) {
                        hasScatter = true;
                        break;
                    }
                }
                if (!hasScatter) {
                    int row = rng.nextInt(VideoSlotGrid.ROWS);
                    symbols[reel][row] = VideoSlotSymbol.SCATTER;
                }
                placed++;
            }
        }

        return new VideoSlotGrid(symbols, bombs);
    }

    private static int rollBombMultiplier(Random rng) {
        int totalWeight = 0;
        for (int w : BOMB_WEIGHTS) totalWeight += w;
        int roll = rng.nextInt(totalWeight);
        int accum = 0;
        for (int i = 0; i < BOMB_VALUES.length; i++) {
            accum += BOMB_WEIGHTS[i];
            if (roll < accum) {
                return BOMB_VALUES[i];
            }
        }
        return 2;
    }

    public static SpinEvaluation evaluateSpin(VideoSlotGrid grid, long betAmount) {
        List<WinningWay> ways = new ArrayList<>();
        long baseWin = 0;

        // 1. Evaluate 243-Ways for each paying symbol
        for (VideoSlotSymbol target : VideoSlotSymbol.values()) {
            if (!target.isPayingSymbol()) continue;

            int consecutiveReels = 0;
            int totalWays = 1;

            for (int reel = 0; reel < VideoSlotGrid.REELS; reel++) {
                int matchesOnReel = 0;
                for (int row = 0; row < VideoSlotGrid.ROWS; row++) {
                    VideoSlotSymbol sym = grid.getSymbol(reel, row);
                    if (sym == target || sym == VideoSlotSymbol.WILD) {
                        matchesOnReel++;
                    }
                }

                if (matchesOnReel > 0) {
                    consecutiveReels++;
                    totalWays *= matchesOnReel;
                } else {
                    break; // Left-to-right requirement
                }
            }

            if (consecutiveReels >= 3) {
                double symMultiplier = target.getPayoutMultiplier(consecutiveReels);
                // Base bet is distributed across 20 payline equivalents
                long payout = Math.round((betAmount / 20.0) * symMultiplier * totalWays);
                if (payout > 0) {
                    ways.add(new WinningWay(target, consecutiveReels, totalWays, payout));
                    baseWin += payout;
                }
            }
        }

        // 2. Evaluate Scatters
        int scatterCount = 0;
        for (int reel = 0; reel < VideoSlotGrid.REELS; reel++) {
            for (int row = 0; row < VideoSlotGrid.ROWS; row++) {
                if (grid.getSymbol(reel, row) == VideoSlotSymbol.SCATTER) {
                    scatterCount++;
                }
            }
        }

        long scatterPayout = 0;
        if (scatterCount == 3) {
            scatterPayout = betAmount * 3L;
        } else if (scatterCount == 4) {
            scatterPayout = betAmount * 15L;
        } else if (scatterCount >= 5) {
            scatterPayout = betAmount * 100L;
        }
        boolean triggeredBonus = scatterCount >= 3;

        // 3. Evaluate Multipliers
        int totalMultiplier = 0;
        for (int reel = 0; reel < VideoSlotGrid.REELS; reel++) {
            for (int row = 0; row < VideoSlotGrid.ROWS; row++) {
                if (grid.getSymbol(reel, row) == VideoSlotSymbol.MULTIPLIER) {
                    totalMultiplier += grid.getMultiplierBomb(reel, row);
                }
            }
        }
        if (totalMultiplier == 0) {
            totalMultiplier = 1;
        }

        // Multipliers multiply ways wins; scatter payouts are added directly
        long waysWin = baseWin * (long) totalMultiplier;
        long totalWin = waysWin + scatterPayout;

        return new SpinEvaluation(grid, ways, baseWin, totalMultiplier, totalWin, scatterCount, triggeredBonus);
    }
}
