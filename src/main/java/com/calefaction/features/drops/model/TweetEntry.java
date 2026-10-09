package com.calefaction.features.drops.model;

import java.time.Instant;
import java.util.List;

public record TweetEntry(
        String id,
        String screenName,
        String userName,
        String profileImageUrl,
        String text,
        Instant createdAt,
        List<String> urls,
        List<String> mediaUrls
) {
    public String getTweetUrl() {
        return "https://x.com/" + screenName + "/status/" + id;
    }
}
