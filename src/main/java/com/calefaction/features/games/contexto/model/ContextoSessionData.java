package com.calefaction.features.games.contexto.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record ContextoSessionData(
        String userId,
        int gameId,
        boolean daily,
        List<ContextoGuess> guesses,
        Instant createdAt,
        Instant lastActiveAt,
        boolean won,
        boolean givenUp,
        boolean announcedVictory,
        boolean announcedStart,
        Set<String> sharedChannelIds,
        String channelId,
        String guildId,
        String secretWord,
        int restoredTotalGuesses,
        int restoredHintsUsed
) {}
