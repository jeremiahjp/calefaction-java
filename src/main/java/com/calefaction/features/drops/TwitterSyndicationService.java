package com.calefaction.features.drops;

import com.calefaction.features.drops.model.TweetEntry;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class TwitterSyndicationService {

    private static final Logger log = LoggerFactory.getLogger(TwitterSyndicationService.class);
    private static final String BASE_URL = "https://syndication.twitter.com";
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>", Pattern.DOTALL);
    private static final DateTimeFormatter TWITTER_DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH
    ).withZone(ZoneOffset.UTC);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public TwitterSyndicationService(WebClient.Builder webClientBuilder, @Autowired(required = false) ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                .build();
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    public TwitterSyndicationService(WebClient.Builder webClientBuilder) {
        this(webClientBuilder, null);
    }

    private final java.util.concurrent.ConcurrentHashMap<String, Instant> rateLimitResetUntil = new java.util.concurrent.ConcurrentHashMap<>();

    public CompletableFuture<List<TweetEntry>> fetchRecentTweets(String screenName) {
        return fetchTimelineResult(screenName).thenApply(com.calefaction.features.drops.model.TwitterFetchResult::tweets);
    }

    public CompletableFuture<com.calefaction.features.drops.model.TwitterFetchResult> fetchTimelineResult(String screenName) {
        Instant resetTime = rateLimitResetUntil.get(screenName);
        if (resetTime != null) {
            if (Instant.now().isBefore(resetTime)) {
                String msg = "Rate-limited until " + resetTime;
                log.warn("🔴 [@{}] Twitter syndication check skipped: {} (HTTP 429 backoff)", screenName, msg);
                return CompletableFuture.completedFuture(
                        com.calefaction.features.drops.model.TwitterFetchResult.rateLimited(screenName, msg)
                );
            } else {
                rateLimitResetUntil.remove(screenName);
            }
        }

        return webClient.get()
                .uri("/srv/timeline-profile/screen-name/{screenName}", screenName)
                .retrieve()
                .bodyToMono(String.class)
                .map(html -> {
                    List<TweetEntry> tweets = parseTweetsFromHtml(html, screenName);
                    return com.calefaction.features.drops.model.TwitterFetchResult.success(screenName, tweets);
                })
                .onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests.class, e -> {
                    String resetEpochStr = e.getHeaders().getFirst("x-rate-limit-reset");
                    Instant resetTimeInstant = null;
                    if (resetEpochStr != null) {
                        try {
                            long epoch = Long.parseLong(resetEpochStr);
                            resetTimeInstant = Instant.ofEpochSecond(epoch);
                        } catch (NumberFormatException ignored) {}
                    }
                    if (resetTimeInstant == null) {
                        resetTimeInstant = Instant.now().plusSeconds(15 * 60); // Default to 15m if header missing
                    }
                    rateLimitResetUntil.put(screenName, resetTimeInstant);
                    String msg = "HTTP 429 Too Many Requests (rate-limited until " + resetTimeInstant + ")";
                    log.warn("🔴 [@{}] Twitter syndication check failed: {}", screenName, msg);
                    return reactor.core.publisher.Mono.just(
                            com.calefaction.features.drops.model.TwitterFetchResult.rateLimited(screenName, msg)
                    );
                })
                .onErrorResume(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    log.warn("🔴 [@{}] Twitter syndication check failed: {}", screenName, msg);
                    return reactor.core.publisher.Mono.just(
                            com.calefaction.features.drops.model.TwitterFetchResult.failed(screenName, msg)
                    );
                })
                .toFuture();
    }

    public List<TweetEntry> parseTweetsFromHtml(String html, String defaultScreenName) {
        if (html == null || html.isBlank()) {
            return List.of();
        }

        Matcher matcher = NEXT_DATA_PATTERN.matcher(html);
        if (!matcher.find()) {
            log.warn("No __NEXT_DATA__ payload found in Twitter syndication response for @{}", defaultScreenName);
            return List.of();
        }

        String json = matcher.group(1);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode entries = root.path("props").path("pageProps").path("timeline").path("entries");
            if (!entries.isArray()) {
                return List.of();
            }

            List<TweetEntry> tweets = new ArrayList<>();
            for (JsonNode entry : entries) {
                JsonNode tweetNode = entry.path("content").path("tweet");
                if (tweetNode.isMissingNode() || tweetNode.isNull()) {
                    continue;
                }

                String id = tweetNode.path("id_str").asText();
                if (id == null || id.isBlank()) {
                    id = tweetNode.path("id").asText();
                }

                String rawText = tweetNode.path("full_text").asText("");
                String createdAtStr = tweetNode.path("created_at").asText();
                Instant createdAt = parseTwitterDate(createdAtStr);

                JsonNode userNode = tweetNode.path("user");
                String screenName = userNode.path("screen_name").asText(defaultScreenName);
                String userName = userNode.path("name").asText(screenName);
                String profileImageUrl = userNode.path("profile_image_url_https").asText(null);
                if (profileImageUrl != null) {
                    profileImageUrl = profileImageUrl.replace("_normal.", "_bigger.");
                }

                List<String> urls = new ArrayList<>();
                String expandedText = rawText;
                JsonNode urlEntities = tweetNode.path("entities").path("urls");
                if (urlEntities.isArray()) {
                    for (JsonNode urlObj : urlEntities) {
                        String shortUrl = urlObj.path("url").asText("");
                        String expandedUrl = urlObj.path("expanded_url").asText("");
                        if (!expandedUrl.isBlank()) {
                            urls.add(expandedUrl);
                            if (!shortUrl.isBlank() && expandedText.contains(shortUrl)) {
                                expandedText = expandedText.replace(shortUrl, expandedUrl);
                            }
                        }
                    }
                }

                List<String> mediaUrls = new ArrayList<>();
                JsonNode mediaEntities = tweetNode.path("extended_entities").path("media");
                if (!mediaEntities.isArray() || mediaEntities.isEmpty()) {
                    mediaEntities = tweetNode.path("entities").path("media");
                }
                if (mediaEntities.isArray()) {
                    for (JsonNode mediaObj : mediaEntities) {
                        String mediaUrl = mediaObj.path("media_url_https").asText("");
                        if (!mediaUrl.isBlank()) {
                            mediaUrls.add(mediaUrl);
                        }
                    }
                }

                tweets.add(new TweetEntry(
                        id,
                        screenName,
                        userName,
                        profileImageUrl,
                        expandedText,
                        createdAt,
                        urls,
                        mediaUrls
                ));
            }
            return tweets;
        } catch (Exception e) {
            log.error("Failed to parse syndication JSON for @{}: {}", defaultScreenName, e.getMessage());
            return List.of();
        }
    }

    private Instant parseTwitterDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.from(TWITTER_DATE_FORMATTER.parse(dateStr));
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
