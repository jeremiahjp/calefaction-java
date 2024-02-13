package com.jp.calefaction.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModerationResponse {
    private String id;
    private String model;
    private List<Result> results;

    // Getters and Setters
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Result {
        private boolean flagged;
        private Categories categories;
        private CategoryScores category_scores;

        // Getters and Setters
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Categories {
        private boolean sexual;
        private boolean hate;
        private boolean harassment;

        @JsonProperty("self-harm")
        private boolean selfHarm;

        @JsonProperty("sexual/minors")
        private boolean sexualMinors;

        @JsonProperty("hate/threatening")
        private boolean hateThreatening;

        @JsonProperty("violence/graphic")
        private boolean violenceGraphic;

        @JsonProperty("self-harm/intent")
        private boolean selfHarmIntent;

        @JsonProperty("self-harm/instructions")
        private boolean selfHarmInstructions;

        @JsonProperty("harassment/threatening")
        private boolean harassmentThreatening;

        private boolean violence;

        // Getters and Setters
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CategoryScores {
        private double sexual;
        private double hate;
        private double harassment;
        private double selfHarm;
        private double sexualMinors;
        private double hateThreatening;
        private double violenceGraphic;
        private double selfHarmIntent;
        private double selfHarmInstructions;
        private double harassmentThreatening;
        private double violence;

        // Getters and Setters
    }
}
