package com.calefaction.features.games.contexto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextoTopResponse(
        List<String> words
) {
}
