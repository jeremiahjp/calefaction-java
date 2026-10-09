package com.calefaction.features.economy.model;

import java.time.Instant;

public record UserWallet(
        String userId,
        long balance,
        long totalWon,
        long totalBet,
        int spinsPlayed,
        Instant lastDailyClaim,
        int dailyStreak
) {
    public static UserWallet createDefault(String userId) {
        return new UserWallet(userId, 1000L, 0L, 0L, 0, null, 0);
    }

    public UserWallet withBalance(long newBalance) {
        return new UserWallet(userId, newBalance, totalWon, totalBet, spinsPlayed, lastDailyClaim, dailyStreak);
    }

    public UserWallet withWin(long winAmount) {
        return new UserWallet(userId, balance + winAmount, totalWon + winAmount, totalBet, spinsPlayed + 1, lastDailyClaim, dailyStreak);
    }

    public UserWallet withBet(long betAmount) {
        return new UserWallet(userId, Math.max(0, balance - betAmount), totalWon, totalBet + betAmount, spinsPlayed + 1, lastDailyClaim, dailyStreak);
    }

    public UserWallet withDaily(long reward, Instant claimTime, int newStreak) {
        return new UserWallet(userId, balance + reward, totalWon, totalBet, spinsPlayed, claimTime, newStreak);
    }

    public UserWallet withBailout(long bailoutAmount) {
        return new UserWallet(userId, balance + bailoutAmount, totalWon, totalBet, spinsPlayed, lastDailyClaim, dailyStreak);
    }
}
