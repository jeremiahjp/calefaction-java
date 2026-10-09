package com.calefaction.features.games.megaslots;

public class VideoSlotGrid {

    public static final int REELS = 5;
    public static final int ROWS = 3;

    private final VideoSlotSymbol[][] grid; // [reel][row]
    private final int[][] multiplierBombs; // [reel][row] value if symbol is MULTIPLIER

    public VideoSlotGrid(VideoSlotSymbol[][] grid, int[][] multiplierBombs) {
        this.grid = grid;
        this.multiplierBombs = multiplierBombs;
    }

    public VideoSlotSymbol getSymbol(int reel, int row) {
        return grid[reel][row];
    }

    public int getMultiplierBomb(int reel, int row) {
        return multiplierBombs[reel][row];
    }

    public String render() {
        return renderWithSpinningReels(REELS);
    }

    public String renderWithSpinningReels(int stoppedReels) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════╗\n");
        for (int row = 0; row < ROWS; row++) {
            sb.append("║ ");
            for (int reel = 0; reel < REELS; reel++) {
                if (reel < stoppedReels) {
                    VideoSlotSymbol sym = grid[reel][row];
                    sb.append(sym.getEmoji());
                } else {
                    sb.append("🌀");
                }
                if (reel < REELS - 1) {
                    sb.append(" │ ");
                }
            }
            sb.append(" ║\n");
        }
        sb.append("╚═══════════════════════════╝");

        // List active multiplier bombs below grid cleanly so box lines stay aligned
        java.util.List<String> bombNotes = new java.util.ArrayList<>();
        for (int reel = 0; reel < Math.min(stoppedReels, REELS); reel++) {
            for (int row = 0; row < ROWS; row++) {
                if (grid[reel][row] == VideoSlotSymbol.MULTIPLIER) {
                    int mult = multiplierBombs[reel][row];
                    bombNotes.add(String.format("Reel %d (x%d)", reel + 1, mult));
                }
            }
        }
        if (!bombNotes.isEmpty()) {
            sb.append("\n💥 Multipliers: ").append(String.join(" • ", bombNotes));
        }

        return sb.toString();
    }
}
