package com.calefaction.features.games.contexto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextoGiveUpResponse(
        Integer distance,
        String lemma,
        String word
) {
}
