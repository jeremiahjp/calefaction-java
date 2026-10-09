package com.calefaction.features.drops;

import static org.junit.jupiter.api.Assertions.*;

import com.calefaction.features.drops.model.TweetEntry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

class TwitterSyndicationServiceTest {

    private TwitterSyndicationService service;

    @BeforeEach
    void setUp() {
        service = new TwitterSyndicationService(WebClient.builder(), JsonMapper.builder().build());
    }

    @Test
    void testParseTweetsFromHtml_Success() {
        String sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
            <script id="__NEXT_DATA__" type="application/json">
            {
              "props": {
                "pageProps": {
                  "timeline": {
                    "entries": [
                      {
                        "content": {
                          "tweet": {
                            "id_str": "1234567890",
                            "full_text": "Pokémon TCG Prismatic Evolutions ETB live at Amazon! https://t.co/abc123xyz",
                            "created_at": "Fri Sep 18 13:42:16 +0000 2026",
                            "user": {
                              "name": "Pokemon Deals",
                              "screen_name": "PokemonDealsTCG",
                              "profile_image_url_https": "https://pbs.twimg.com/avatar_normal.jpg"
                            },
                            "entities": {
                              "urls": [
                                {
                                  "url": "https://t.co/abc123xyz",
                                  "expanded_url": "https://www.amazon.com/dp/B0XYZ123",
                                  "display_url": "amazon.com/dp/B0XYZ123"
                                }
                              ],
                              "media": [
                                {
                                  "media_url_https": "https://pbs.twimg.com/media/photo1.jpg"
                                }
                              ]
                            }
                          }
                        }
                      }
                    ]
                  }
                }
              }
            }
            </script>
            </head>
            <body></body>
            </html>
            """;

        List<TweetEntry> tweets = service.parseTweetsFromHtml(sampleHtml, "PokemonDealsTCG");
        assertNotNull(tweets);
        assertEquals(1, tweets.size());

        TweetEntry tweet = tweets.get(0);
        assertEquals("1234567890", tweet.id());
        assertEquals("PokemonDealsTCG", tweet.screenName());
        assertEquals("Pokemon Deals", tweet.userName());
        assertTrue(tweet.profileImageUrl().contains("_bigger.jpg"));
        assertTrue(tweet.text().contains("https://www.amazon.com/dp/B0XYZ123"), "Should expand t.co links");
        assertEquals("https://x.com/PokemonDealsTCG/status/1234567890", tweet.getTweetUrl());
        assertEquals(1, tweet.urls().size());
        assertEquals(1, tweet.mediaUrls().size());
        assertEquals("https://pbs.twimg.com/media/photo1.jpg", tweet.mediaUrls().get(0));
    }

    @Test
    void testParseTweetsFromHtml_EmptyOrInvalid() {
        assertTrue(service.parseTweetsFromHtml(null, "Drop_Notify").isEmpty());
        assertTrue(service.parseTweetsFromHtml("", "Drop_Notify").isEmpty());
        assertTrue(service.parseTweetsFromHtml("<html>no next data</html>", "Drop_Notify").isEmpty());
    }
}
