package com.calefaction.features.games.contexto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextoGuessResponse(
        Integer distance,
        String lemma,
        String word,
        String error
) {
    public boolean isError() {
        return error != null && !error.isBlank();
    }
}
