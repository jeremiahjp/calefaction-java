package com.calefaction.features.drops.model;

import java.util.List;

public record TwitterFetchResult(
        String screenName,
        List<TweetEntry> tweets,
        Status status,
        String message
) {
    public enum Status {
        SUCCESS,
        RATE_LIMITED,
        FAILED
    }

    public static TwitterFetchResult success(String screenName, List<TweetEntry> tweets) {
        return new TwitterFetchResult(screenName, tweets, Status.SUCCESS, null);
    }

    public static TwitterFetchResult rateLimited(String screenName, String message) {
        return new TwitterFetchResult(screenName, List.of(), Status.RATE_LIMITED, message);
    }

    public static TwitterFetchResult failed(String screenName, String message) {
        return new TwitterFetchResult(screenName, List.of(), Status.FAILED, message);
    }
}
