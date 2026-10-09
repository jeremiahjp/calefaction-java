package com.calefaction.features.games.contexto;

import com.calefaction.features.games.contexto.dto.ContextoGiveUpResponse;
import com.calefaction.features.games.contexto.dto.ContextoGuessResponse;
import com.calefaction.features.games.contexto.dto.ContextoTipResponse;
import com.calefaction.features.games.contexto.dto.ContextoTopResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ContextoService {

    private static final Logger log = LoggerFactory.getLogger(ContextoService.class);
    private static final String BASE_URL = "https://api.contexto.me/machado/en";
    private static final LocalDate START_DATE_EN = LocalDate.of(2022, 9, 18);

    private final WebClient webClient;
    private final ZoneId zoneId;

    @Autowired
    public ContextoService(
            WebClient.Builder webClientBuilder,
            @Value("${contexto.timezone:America/Chicago}") String timezone
    ) {
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .build();
        ZoneId parsedZone;
        try {
            parsedZone = ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("Invalid timezone '{}' configured for Contexto, falling back to America/Chicago", timezone);
            parsedZone = ZoneId.of("America/Chicago");
        }
        this.zoneId = parsedZone;
        log.info("Contexto daily game rollover configured with timezone: {}", this.zoneId);
    }

    public ContextoService(WebClient.Builder webClientBuilder) {
        this(webClientBuilder, "America/Chicago");
    }

    /**
     * Calculates the daily puzzle ID based on days since 2022-09-18 in configured timezone (default: America/Chicago).
     */
    public int getDailyGameId() {
        LocalDate today = LocalDate.now(zoneId);
        return (int) ChronoUnit.DAYS.between(START_DATE_EN, today);
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    /**
     * Scores a word guess against a given game ID.
     */
    public Mono<ContextoGuessResponse> guess(int gameId, String rawWord) {
        String word = sanitizeWord(rawWord);
        if (word.isEmpty()) {
            return Mono.just(new ContextoGuessResponse(null, null, rawWord, "Word must contain valid English letters."));
        }

        return webClient.get()
                .uri("/game/{gameId}/{word}", gameId, word)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(ContextoGuessResponse.class);
                    } else if (response.statusCode().value() == 404 || response.statusCode().value() == 400) {
                        return response.bodyToMono(ContextoGuessResponse.class)
                                .map(res -> new ContextoGuessResponse(null, null, word,
                                        res.error() != null ? res.error() : "Unknown word."));
                    } else {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.warn("Contexto API returned {}: {}", response.statusCode(), body);
                                    return Mono.just(new ContextoGuessResponse(null, null, word, "Contexto service unavailable."));
                                });
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Network error guessing word '{}': {}", word, e.getMessage());
                    return Mono.just(new ContextoGuessResponse(null, null, word, "Connection error. Try again."));
                });
    }

    /**
     * Retrieves a hint word at or near the specified target rank.
     */
    public Mono<ContextoTipResponse> getHint(int gameId, int targetDistance) {
        return webClient.get()
                .uri("/tip/{gameId}/{distance}", gameId, targetDistance)
                .retrieve()
                .bodyToMono(ContextoTipResponse.class)
                .doOnError(e -> log.warn("Failed to get hint for game {} at distance {}: {}", gameId, targetDistance, e.getMessage()));
    }

    /**
     * Forfeits the game and retrieves the secret target word.
     */
    public Mono<ContextoGiveUpResponse> giveUp(int gameId) {
        return webClient.get()
                .uri("/giveup/{gameId}", gameId)
                .retrieve()
                .bodyToMono(ContextoGiveUpResponse.class)
                .doOnError(e -> log.warn("Failed to give up for game {}: {}", gameId, e.getMessage()));
    }

    /**
     * Retrieves the top closest words for the puzzle.
     */
    public Mono<ContextoTopResponse> getTopWords(int gameId) {
        return webClient.get()
                .uri("/top/{gameId}", gameId)
                .retrieve()
                .bodyToMono(ContextoTopResponse.class)
                .doOnError(e -> log.warn("Failed to get top words for game {}: {}", gameId, e.getMessage()));
    }

    public static String sanitizeWord(String word) {
        if (word == null) {
            return "";
        }
        return word.trim().toLowerCase().replaceAll("[^a-z]", "");
    }
}
