package com.calefaction.features.games.contexto.model;

import java.time.Instant;

public record ContextoGuess(
        String word,
        int distance,
        Instant timestamp,
        boolean isHint
) {
    public boolean isMatch() {
        return distance == 0 || distance == 1; // distance 0 or 1 indicates the target word
    }
}
