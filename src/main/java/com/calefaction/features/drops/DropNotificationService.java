package com.calefaction.features.drops;

import com.calefaction.features.drops.model.TweetEntry;
import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DropNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DropNotificationService.class);

    public record MonitoredAccount(String screenName, boolean filterPokemon) {}

    public static final List<MonitoredAccount> DEFAULT_ACCOUNTS = List.of(
            new MonitoredAccount("PokemonDealsTCG", false),
            new MonitoredAccount("Drop_Notify", true),
            new MonitoredAccount("jjuddpi", false)
    );

    private final TwitterSyndicationService twitterService;
    private final DropProperties dropProperties;
    private final JdbcClient jdbcClient;
    private final JDA jda;
    private final Set<String> seenTweetIds = ConcurrentHashMap.newKeySet();
    private Instant lastPollTime = null;
    private boolean initialized = false;

    @Autowired
    public DropNotificationService(
            TwitterSyndicationService twitterService,
            DropProperties dropProperties,
            @Autowired(required = false) JdbcClient jdbcClient,
            @Autowired(required = false) JDA jda
    ) {
        this.twitterService = twitterService;
        this.dropProperties = dropProperties;
        this.jdbcClient = jdbcClient;
        this.jda = jda;
    }

    public DropNotificationService(
            TwitterSyndicationService twitterService,
            DropProperties dropProperties
    ) {
        this(twitterService, dropProperties, null, null);
    }

    @PostConstruct
    public void init() {
        loadSeenTweetsFromDatabase();
    }

    private void loadSeenTweetsFromDatabase() {
        if (jdbcClient == null) {
            return;
        }
        try {
            var rows = jdbcClient.sql("SELECT tweet_id FROM drop_tweet_notifications")
                    .query((rs, rowNum) -> rs.getString("tweet_id"))
                    .list();
            seenTweetIds.addAll(rows);
            log.info("Loaded {} seen drop tweet IDs from database", rows.size());
        } catch (Exception e) {
            log.warn("Could not load seen drop tweet IDs from database: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${drops.poll-interval-seconds:45}000", initialDelay = 15000)
    public void pollDrops() {
        if (!dropProperties.isEnabled()) {
            return;
        }
        log.info("Starting scheduled drop monitor poll across {} accounts...", DEFAULT_ACCOUNTS.size());
        checkDrops().whenComplete((newDrops, ex) -> {
            if (ex != null) {
                log.error("🔴 Drop poll encountered an unexpected error: {}", ex.getMessage());
            }
        });
    }

    public CompletableFuture<List<TweetEntry>> checkDrops() {
        this.lastPollTime = Instant.now();
        List<CompletableFuture<List<TweetEntry>>> futures = new ArrayList<>();

        for (MonitoredAccount acc : DEFAULT_ACCOUNTS) {
            CompletableFuture<List<TweetEntry>> future = twitterService.fetchTimelineResult(acc.screenName())
                    .thenApply(fetchResult -> processAccountFetchResult(acc, fetchResult));
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<TweetEntry> allNewDrops = new ArrayList<>();
                    for (var f : futures) {
                        allNewDrops.addAll(f.join());
                    }
                    if (!initialized) {
                        initialized = true;
                        log.info("Drop monitor initial baseline seeding completed. Monitored accounts: {}",
                                DEFAULT_ACCOUNTS.stream().map(MonitoredAccount::screenName).toList());
                    }
                    if (!allNewDrops.isEmpty()) {
                        log.info("🟢 Drop poll complete: Detected and posted {} new drop alert(s)!", allNewDrops.size());
                    }
                    return allNewDrops;
                });
    }

    private List<TweetEntry> processAccountFetchResult(MonitoredAccount account, com.calefaction.features.drops.model.TwitterFetchResult fetchResult) {
        if (fetchResult.status() == com.calefaction.features.drops.model.TwitterFetchResult.Status.RATE_LIMITED) {
            log.warn("🔴 [@{}] Check failed: rate-limited by Twitter (429): {}", account.screenName(), fetchResult.message());
            return List.of();
        }
        if (fetchResult.status() == com.calefaction.features.drops.model.TwitterFetchResult.Status.FAILED) {
            log.warn("🔴 [@{}] Check failed: unable to fetch timeline: {}", account.screenName(), fetchResult.message());
            return List.of();
        }

        List<TweetEntry> tweets = fetchResult.tweets();
        if (tweets == null || tweets.isEmpty()) {
            log.warn("🔴 [@{}] Check failed: received empty timeline response or no tweets parsed", account.screenName());
            return List.of();
        }

        // Only inspect the single most recent tweet on the account's timeline
        TweetEntry tweet = tweets.get(0);

        if (seenTweetIds.contains(tweet.id())) {
            log.info("🟡 [@{}] Check successful: No new tweets (most recent id={})", account.screenName(), tweet.id());
            return List.of();
        }

        String matchedKeyword = null;
        if (account.filterPokemon()) {
            matchedKeyword = getMatchedKeyword(tweet.text());
            if (matchedKeyword == null) {
                // Not Pokemon related, mark seen so we don't re-check it
                recordTweetSeen(tweet.id(), account.screenName(), tweet.text(), null, tweet.createdAt());
                log.info("🟡 [@{}] Check successful: Tweet id={} ignored (not Pokémon related): \"{}\"",
                        account.screenName(), tweet.id(), abbreviate(tweet.text(), 60));
                return List.of();
            }
        } else {
            matchedKeyword = getMatchedKeyword(tweet.text());
            if (matchedKeyword == null) {
                matchedKeyword = "jjuddpi".equalsIgnoreCase(account.screenName()) ? "TEST ALERT" : "POKÉMON TCG";
            }
        }

        if (!initialized) {
            log.info("Baseline seed for @{}: tweet id={} - \"{}\"",
                    account.screenName(), tweet.id(), abbreviate(tweet.text(), 60));
            // Seed initial most recent tweet without sending an alert on startup
            recordTweetSeen(tweet.id(), account.screenName(), tweet.text(), matchedKeyword, tweet.createdAt());
            return List.of();
        }

        // Valid new drop detected on the most recent tweet!
        log.info("🟢 [@{}] Check successful! Found new tweet (id={}, keyword={}): \"{}\"",
                account.screenName(), tweet.id(), matchedKeyword, abbreviate(tweet.text(), 80));
        recordTweetSeen(tweet.id(), account.screenName(), tweet.text(), matchedKeyword, tweet.createdAt());
        sendDropNotification(tweet, matchedKeyword);

        return List.of(tweet);
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null) return "";
        String singleLine = text.replace("\n", " ").replace("\r", " ").trim();
        return singleLine.length() <= maxLen ? singleLine : singleLine.substring(0, maxLen - 3) + "...";
    }

    private void recordTweetSeen(String tweetId, String account, String text, String keyword, Instant createdAt) {
        seenTweetIds.add(tweetId);
        if (jdbcClient != null) {
            try {
                jdbcClient.sql("""
                    INSERT INTO drop_tweet_notifications (tweet_id, account, tweet_text, matched_keyword, created_at, notified_at)
                    VALUES (?, ?, ?, ?, ?, NOW())
                    ON CONFLICT (tweet_id) DO NOTHING
                """)
                .params(
                        tweetId,
                        account,
                        text != null ? text : "",
                        keyword,
                        createdAt != null ? Timestamp.from(createdAt) : null
                )
                .update();
            } catch (Exception e) {
                log.error("Failed to persist drop tweet {} to database: {}", tweetId, e.getMessage());
            }
        }
    }

    public void sendDropNotification(TweetEntry tweet, String matchedKeyword) {
        if (jda == null) {
            log.info("JDA is null, simulated drop notification for tweet {}: {}", tweet.id(), tweet.text());
            return;
        }

        List<String> channelIds = dropProperties.getChannelIds();
        if (channelIds.isEmpty()) {
            log.warn("Pokémon drop detected from @{}, but no channel_ids configured in drops.channel_ids!", tweet.screenName());
            return;
        }

        String mentionContent = DropRenderer.buildMentionString(
                dropProperties.getMentionUserIds(),
                dropProperties.getMentionRoleIds()
        );

        var embed = DropRenderer.buildDropEmbed(tweet, matchedKeyword);
        var actionRows = DropRenderer.buildActionRows(tweet);

        for (String channelId : channelIds) {
            try {
                MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId.trim());
                if (channel != null) {
                    var action = channel.sendMessageEmbeds(embed);
                    if (!mentionContent.isBlank()) {
                        action = channel.sendMessage(mentionContent).setEmbeds(embed);
                    }
                    if (!actionRows.isEmpty()) {
                        action = action.setComponents(actionRows);
                    }
                    action.queue(
                            msg -> log.info("Posted drop alert {} to channel {}", tweet.id(), channelId),
                            err -> log.error("Failed to send drop alert to channel {}: {}", channelId, err.getMessage())
                    );
                } else {
                    log.warn("Configured drop channel ID {} not found or bot lacks view permission", channelId);
                }
            } catch (Exception e) {
                log.error("Error sending drop alert to channel {}: {}", channelId, e.getMessage());
            }
        }
    }

    public boolean isPokemonRelated(String text) {
        return getMatchedKeyword(text) != null;
    }

    public String getMatchedKeyword(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ENGLISH);
        for (String kw : dropProperties.getPokemonKeywords()) {
            if (kw != null && !kw.isBlank() && lower.contains(kw.toLowerCase(Locale.ENGLISH))) {
                return kw;
            }
        }
        return null;
    }

    public Instant getLastPollTime() {
        return lastPollTime;
    }

    public Set<String> getSeenTweetIds() {
        return seenTweetIds;
    }
}
