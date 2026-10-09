package com.calefaction.features.drops;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "drops")
public class DropProperties {

    private boolean enabled = true;
    private int pollIntervalSeconds = 45;
    private List<String> channelIds = new ArrayList<>();
    private List<String> mentionUserIds = new ArrayList<>(List.of("94220323628523520"));
    private List<String> mentionRoleIds = new ArrayList<>();
    private List<String> pokemonKeywords = new ArrayList<>(List.of(
            "pokemon",
            "pokémon",
            "tcg",
            "etb",
            "booster",
            "elite trainer box",
            "prismatic",
            "charizard",
            "pikachu",
            "151",
            "surging sparks",
            "stellar crown",
            "twilight masquerade",
            "paldean fates",
            "paradox rift",
            "obsidian flames",
            "scarlet",
            "violet",
            "blooming waters",
            "ultra premium",
            "upc",
            "pokecenter",
            "pokemon center",
            "celebration"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public List<String> getChannelIds() {
        return channelIds;
    }

    public void setChannelIds(List<String> channelIds) {
        if (channelIds == null) {
            this.channelIds = new ArrayList<>();
            return;
        }
        List<String> clean = new ArrayList<>();
        for (String c : channelIds) {
            if (c != null) {
                for (String part : c.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        clean.add(trimmed);
                    }
                }
            }
        }
        this.channelIds = clean;
    }

    public List<String> getMentionUserIds() {
        return mentionUserIds;
    }

    public void setMentionUserIds(List<String> mentionUserIds) {
        if (mentionUserIds == null) {
            this.mentionUserIds = new ArrayList<>();
            return;
        }
        List<String> clean = new ArrayList<>();
        for (String u : mentionUserIds) {
            if (u != null) {
                for (String part : u.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        clean.add(trimmed);
                    }
                }
            }
        }
        this.mentionUserIds = clean;
    }

    public List<String> getMentionRoleIds() {
        return mentionRoleIds;
    }

    public void setMentionRoleIds(List<String> mentionRoleIds) {
        this.mentionRoleIds = mentionRoleIds != null ? mentionRoleIds : new ArrayList<>();
    }

    public List<String> getPokemonKeywords() {
        return pokemonKeywords;
    }

    public void setPokemonKeywords(List<String> pokemonKeywords) {
        this.pokemonKeywords = pokemonKeywords != null ? pokemonKeywords : new ArrayList<>();
    }
}
