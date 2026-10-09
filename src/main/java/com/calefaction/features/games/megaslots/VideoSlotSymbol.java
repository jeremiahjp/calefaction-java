package com.calefaction.features.games.megaslots;

public enum VideoSlotSymbol {
    CROWN("👑", "Crown", 25.0, 75.0, 250.0, 5),
    DIAMOND("💎", "Diamond", 15.0, 45.0, 125.0, 8),
    SEVEN("7️⃣", "Seven", 10.0, 28.0, 75.0, 12),
    BELL("🔔", "Bell", 7.0, 18.0, 50.0, 16),
    FIRE("🔥", "Fire", 5.0, 12.0, 30.0, 20),
    CLOVER("🍀", "Clover", 3.5, 9.0, 22.0, 24),
    GRAPE("🍇", "Grape", 2.5, 6.0, 15.0, 28),
    LEMON("🍋", "Lemon", 1.8, 4.0, 10.0, 32),
    CHERRY("🍒", "Cherry", 1.2, 3.0, 7.0, 36),
    WILD("🃏", "Wild", 0, 0, 0, 6),
    SCATTER("⭐", "Scatter", 0, 0, 0, 6),
    MULTIPLIER("💥", "Multiplier", 0, 0, 0, 4);

    private final String emoji;
    private final String name;
    private final double pay3;
    private final double pay4;
    private final double pay5;
    private final int weight;

    VideoSlotSymbol(String emoji, String name, double pay3, double pay4, double pay5, int weight) {
        this.emoji = emoji;
        this.name = name;
        this.pay3 = pay3;
        this.pay4 = pay4;
        this.pay5 = pay5;
        this.weight = weight;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getName() {
        return name;
    }

    public double getPayoutMultiplier(int matchCount) {
        return switch (matchCount) {
            case 3 -> pay3;
            case 4 -> pay4;
            case 5 -> pay5;
            default -> 0.0;
        };
    }

    public int getWeight() {
        return weight;
    }

    public boolean isPayingSymbol() {
        return this != WILD && this != SCATTER && this != MULTIPLIER;
    }
}
