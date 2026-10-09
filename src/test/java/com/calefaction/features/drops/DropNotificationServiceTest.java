package com.calefaction.features.drops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.calefaction.features.drops.model.TweetEntry;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DropNotificationServiceTest {

    private TwitterSyndicationService mockTwitterService;
    private DropProperties dropProperties;
    private DropNotificationService service;

    @BeforeEach
    void setUp() {
        mockTwitterService = mock(TwitterSyndicationService.class);
        dropProperties = new DropProperties();
        dropProperties.setEnabled(true);
        dropProperties.setChannelIds(List.of("channel-1", "channel-2"));
        dropProperties.setMentionUserIds(List.of("94220323628523520"));

        service = new DropNotificationService(mockTwitterService, dropProperties);
    }

    @Test
    void testKeywordMatching() {
        assertTrue(service.isPokemonRelated("Restock of Pokémon 151 at Target!"));
        assertTrue(service.isPokemonRelated("New ETB bundle up at Best Buy"));
        assertTrue(service.isPokemonRelated("Pikachu ex box live!"));
        assertTrue(service.isPokemonRelated("Charizard UPC available"));
        assertTrue(service.isPokemonRelated("Prismatic Evolutions Booster Bundle"));

        assertFalse(service.isPokemonRelated("PS5 Pro 30th Anniversary Console in stock at Sony"));
        assertFalse(service.isPokemonRelated("RTX 5090 restock at Newegg"));
        assertFalse(service.isPokemonRelated("Nintendo Switch OLED Zelda edition"));
    }

    @Test
    void testCheckDrops_InitialRunSeedsWithoutAlerts() {
        TweetEntry tweet1 = new TweetEntry("t-1", "PokemonDealsTCG", "Pokemon Deals", null, "ETB Restock!", Instant.now(), List.of(), List.of());
        TweetEntry tweet2 = new TweetEntry("t-2", "Drop_Notify", "Drop Notify", null, "PS5 Console drop", Instant.now(), List.of(), List.of());
        TweetEntry tweet3 = new TweetEntry("t-3", "jjuddpi", "JJ", null, "Testing a tweet!", Instant.now(), List.of(), List.of());

        when(mockTwitterService.fetchTimelineResult("PokemonDealsTCG"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("PokemonDealsTCG", List.of(tweet1))));
        when(mockTwitterService.fetchTimelineResult("Drop_Notify"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("Drop_Notify", List.of(tweet2))));
        when(mockTwitterService.fetchTimelineResult("jjuddpi"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("jjuddpi", List.of(tweet3))));

        // First run should seed seen tweets and return 0 new drop alerts
        List<TweetEntry> firstRunDrops = service.checkDrops().join();
        assertEquals(0, firstRunDrops.size());
        assertTrue(service.getSeenTweetIds().contains("t-1"));
        assertTrue(service.getSeenTweetIds().contains("t-2"));
        assertTrue(service.getSeenTweetIds().contains("t-3"));

        // Second run with a brand new tweet on Drop_Notify (most recent)
        TweetEntry newDrop = new TweetEntry("t-4", "Drop_Notify", "Drop Notify", null, "🚨 LIVE: Pokemon Prismatic ETB at Target!", Instant.now(), List.of(), List.of());

        when(mockTwitterService.fetchTimelineResult("PokemonDealsTCG"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("PokemonDealsTCG", List.of(tweet1))));
        when(mockTwitterService.fetchTimelineResult("Drop_Notify"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("Drop_Notify", List.of(newDrop))));
        when(mockTwitterService.fetchTimelineResult("jjuddpi"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("jjuddpi", List.of(tweet3))));

        List<TweetEntry> secondRunDrops = service.checkDrops().join();
        assertEquals(1, secondRunDrops.size());
        assertEquals("t-4", secondRunDrops.get(0).id());
        assertTrue(service.getSeenTweetIds().contains("t-4"));
    }

    @Test
    void testCheckDrops_PokemonDealsTCG_AcceptsAllTweets() {
        // Seed initial run to set initialized flag to true
        TweetEntry seedTweet = new TweetEntry("seed-1", "PokemonDealsTCG", "Pokemon Deals", null, "Initial tweet", Instant.now(), List.of(), List.of());
        when(mockTwitterService.fetchTimelineResult("PokemonDealsTCG"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("PokemonDealsTCG", List.of(seedTweet))));
        when(mockTwitterService.fetchTimelineResult("Drop_Notify"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("Drop_Notify", List.of())));
        when(mockTwitterService.fetchTimelineResult("jjuddpi"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("jjuddpi", List.of())));
        service.checkDrops().join();

        TweetEntry tcgtweet = new TweetEntry("tcg-1", "PokemonDealsTCG", "Pokemon Deals", null, "Check out Amazon invite wave", Instant.now(), List.of(), List.of());

        when(mockTwitterService.fetchTimelineResult("PokemonDealsTCG"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("PokemonDealsTCG", List.of(tcgtweet))));

        List<TweetEntry> drops = service.checkDrops().join();
        assertEquals(1, drops.size());
        assertEquals("tcg-1", drops.get(0).id());
    }

    @Test
    void testCheckDrops_jjuddpi_AcceptsAllTweetsForTesting() {
        // Seed initial run
        TweetEntry seedTweet = new TweetEntry("seed-j", "jjuddpi", "JJ", null, "Seed", Instant.now(), List.of(), List.of());
        when(mockTwitterService.fetchTimelineResult("PokemonDealsTCG"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("PokemonDealsTCG", List.of())));
        when(mockTwitterService.fetchTimelineResult("Drop_Notify"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("Drop_Notify", List.of())));
        when(mockTwitterService.fetchTimelineResult("jjuddpi"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("jjuddpi", List.of(seedTweet))));
        service.checkDrops().join();

        // New tweet on jjuddpi without any pokemon keywords
        TweetEntry myTestTweet = new TweetEntry("tweet-100", "jjuddpi", "JJ", null, "Just testing my discord bot drop alert!", Instant.now(), List.of(), List.of());
        when(mockTwitterService.fetchTimelineResult("jjuddpi"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("jjuddpi", List.of(myTestTweet))));

        List<TweetEntry> drops = service.checkDrops().join();
        assertEquals(1, drops.size());
        assertEquals("tweet-100", drops.get(0).id());
        assertEquals("Just testing my discord bot drop alert!", drops.get(0).text());
    }

    @Test
    void testCheckDrops_RateLimitedAccount_DoesNotCrashAndReturnsEmpty() {
        // Initial run
        TweetEntry seedTweet = new TweetEntry("seed-1", "PokemonDealsTCG", "Pokemon Deals", null, "Seed", Instant.now(), List.of(), List.of());
        when(mockTwitterService.fetchTimelineResult("PokemonDealsTCG"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.success("PokemonDealsTCG", List.of(seedTweet))));
        when(mockTwitterService.fetchTimelineResult("Drop_Notify"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.rateLimited("Drop_Notify", "429 Too Many Requests")));
        when(mockTwitterService.fetchTimelineResult("jjuddpi"))
                .thenReturn(CompletableFuture.completedFuture(com.calefaction.features.drops.model.TwitterFetchResult.failed("jjuddpi", "Connection refused")));
        
        List<TweetEntry> drops = service.checkDrops().join();
        assertEquals(0, drops.size());
    }
}
