package com.calefaction.features.games.contexto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextoTipResponse(
        Integer distance,
        String lemma,
        String word
) {
}
