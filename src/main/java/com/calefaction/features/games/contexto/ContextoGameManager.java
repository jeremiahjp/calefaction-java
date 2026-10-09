package com.calefaction.features.games.contexto;

import com.calefaction.features.games.contexto.model.ContextoGuess;
import com.calefaction.features.games.contexto.model.ContextoSession;
import com.calefaction.features.games.contexto.model.ContextoSessionData;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ContextoGameManager {

    private static final Logger log = LoggerFactory.getLogger(ContextoGameManager.class);
    private final ContextoService contextoService;
    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;
    private Path sessionsStorageFile;
    private final Cache<String, ContextoSession> sessions = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(24))
            .maximumSize(10_000)
            .build();

    @Autowired
    public ContextoGameManager(
            ContextoService contextoService,
            @Autowired(required = false) JdbcClient jdbcClient,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.contextoService = contextoService;
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    public ContextoGameManager(ContextoService contextoService, ObjectMapper objectMapper) {
        this(contextoService, null, objectMapper);
    }

    public ContextoGameManager(ContextoService contextoService) {
        this(contextoService, null, null);
    }

    @PostConstruct
    public void init() {
        loadSessionsFromDatabase();
        initSessionsStorage();
    }

    private synchronized void loadSessionsFromDatabase() {
        if (jdbcClient == null) {
            return;
        }
        try {
            int daily = getDailyGameId();
            var rows = jdbcClient.sql("SELECT data_json FROM contexto_sessions WHERE game_id >= ?")
                    .param(daily - 7)
                    .query((rs, rowNum) -> rs.getString("data_json"))
                    .list();
            int count = 0;
            for (String json : rows) {
                try {
                    ContextoSessionData data = objectMapper.readValue(json, ContextoSessionData.class);
                    sessions.put(data.userId(), ContextoSession.fromData(data));
                    count++;
                } catch (Exception ex) {
                    log.warn("Failed to deserialize session JSON from DB: {}", ex.getMessage());
                }
            }
            log.info("Loaded {} Contexto sessions from database", count);
        } catch (Exception e) {
            log.debug("Could not load sessions from database: {}", e.getMessage());
        }
    }

    private void initSessionsStorage() {
        if (sessionsStorageFile == null) {
            Path haDataDir = Path.of("/data");
            Path baseDir = (Files.exists(haDataDir) && Files.isWritable(haDataDir)) ? haDataDir : Path.of("data");
            try {
                Files.createDirectories(baseDir);
                this.sessionsStorageFile = baseDir.resolve("contexto-sessions.json");
            } catch (Exception e) {
                log.warn("Failed to initialize Contexto sessions storage directory: {}", e.getMessage());
                this.sessionsStorageFile = Path.of("contexto-sessions.json");
            }
        }
        loadSessionsFromFile();
    }

    private synchronized void loadSessionsFromFile() {
        if (sessionsStorageFile != null && Files.exists(sessionsStorageFile)) {
            try {
                Map<String, ContextoSessionData> loaded = objectMapper.readValue(
                        sessionsStorageFile,
                        new TypeReference<Map<String, ContextoSessionData>>() {}
                );
                if (loaded != null) {
                    int daily = getDailyGameId();
                    int count = 0;
                    for (var entry : loaded.entrySet()) {
                        ContextoSessionData data = entry.getValue();
                        if (data != null && data.gameId() >= daily - 7) {
                            sessions.put(entry.getKey(), ContextoSession.fromData(data));
                            count++;
                        }
                    }
                    log.info("Loaded {} Contexto sessions from {}", count, sessionsStorageFile);
                }
            } catch (Exception e) {
                log.warn("Failed to load Contexto sessions from {}: {}", sessionsStorageFile, e.getMessage());
            }
        }
    }

    public synchronized void saveSessionsToFile() {
        if (sessionsStorageFile == null) {
            return;
        }
        try {
            Map<String, ContextoSessionData> toSave = new HashMap<>();
            int daily = getDailyGameId();
            for (var entry : sessions.asMap().entrySet()) {
                ContextoSession session = entry.getValue();
                if (session != null && session.getGameId() >= daily - 7) {
                    toSave.put(entry.getKey(), session.toData());
                }
            }
            Path tmpFile = Path.of(sessionsStorageFile.toString() + ".tmp");
            objectMapper.writeValue(tmpFile, toSave);
            try {
                Files.move(tmpFile, sessionsStorageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                Files.move(tmpFile, sessionsStorageFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("Failed to save Contexto sessions to {}: {}", sessionsStorageFile, e.getMessage());
        }
    }

    void setSessionsStorageFile(Path file) {
        this.sessionsStorageFile = file;
        loadSessionsFromFile();
    }

    public synchronized void persistSession(ContextoSession session) {
        if (session == null) return;
        sessions.put(session.getUserId(), session);
        if (jdbcClient != null) {
            try {
                String json = objectMapper.writeValueAsString(session.toData());
                jdbcClient.sql("""
                    INSERT INTO contexto_sessions (user_id, game_id, is_won, is_given_up, is_daily, data_json, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT (user_id, game_id) DO UPDATE SET
                        is_won = EXCLUDED.is_won,
                        is_given_up = EXCLUDED.is_given_up,
                        is_daily = EXCLUDED.is_daily,
                        data_json = EXCLUDED.data_json,
                        updated_at = NOW()
                """)
                .params(session.getUserId(), session.getGameId(), session.isWon(), session.isGivenUp(), session.isDaily(), json)
                .update();
            } catch (Exception e) {
                log.error("Failed to persist session {} to database: {}", session.getUserId(), e.getMessage());
            }
        }
        saveSessionsToFile();
    }

    public Optional<ContextoSession> getSession(String userId) {
        ContextoSession cached = sessions.getIfPresent(userId);
        if (cached != null) {
            return Optional.of(cached);
        }
        if (jdbcClient != null) {
            try {
                int daily = getDailyGameId();
                var found = jdbcClient.sql("SELECT data_json FROM contexto_sessions WHERE user_id = ? AND game_id = ?")
                        .params(userId, daily)
                        .query((rs, rowNum) -> rs.getString("data_json"))
                        .optional();
                if (found.isPresent()) {
                    ContextoSessionData data = objectMapper.readValue(found.get(), ContextoSessionData.class);
                    ContextoSession session = ContextoSession.fromData(data);
                    sessions.put(userId, session);
                    return Optional.of(session);
                }
            } catch (Exception e) {
                log.debug("Database lookup for session {}: {}", userId, e.getMessage());
            }
        }
        return Optional.empty();
    }

    public int getDailyGameId() {
        return contextoService.getDailyGameId();
    }

    public ZoneId getZoneId() {
        return contextoService.getZoneId();
    }

    public ContextoSession startDailyGame(String userId) {
        int gameId = contextoService.getDailyGameId();
        ContextoSession session = new ContextoSession(userId, gameId, true);
        persistSession(session);
        return session;
    }

    public CompletableFuture<ContextoSession> submitGuess(String userId, String rawWord) {
        ContextoSession session = sessions.getIfPresent(userId);
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (session.isFinished()) {
            session.setStatusMessage("Today's game has already ended! Come back at midnight UTC for tomorrow's puzzle.");
            return CompletableFuture.completedFuture(session);
        }

        String word = ContextoService.sanitizeWord(rawWord);
        if (word.isBlank()) {
            session.setStatusMessage("Please enter a valid word.");
            return CompletableFuture.completedFuture(session);
        }

        if (session.hasGuessed(word)) {
            session.setStatusMessage(String.format("You already guessed **%s**!", word));
            return CompletableFuture.completedFuture(session);
        }

        return contextoService.guess(session.getGameId(), word)
                .map(response -> {
                    if (response.isError() || response.distance() == null) {
                        String errMsg = response.error() != null ? response.error() : "Unknown word.";
                        session.setStatusMessage(String.format("❌ **%s**: %s", word, errMsg));
                    } else {
                        int normalizedDistance = response.distance() + 1;
                        ContextoGuess guess = new ContextoGuess(response.word(), normalizedDistance, Instant.now(), false);
                        session.addGuess(guess);
                        persistSession(session);
                    }
                    return session;
                })
                .toFuture();
    }

    public CompletableFuture<ContextoSession> requestHint(String userId) {
        ContextoSession session = sessions.getIfPresent(userId);
        if (session == null || session.isFinished()) {
            return CompletableFuture.completedFuture(session);
        }

        OptionalInt bestDist = session.getBestDistance();
        int targetRank;
        if (bestDist.isPresent() && bestDist.getAsInt() > 2) {
            targetRank = Math.max(1, (bestDist.getAsInt() - 1) / 2);
        } else {
            targetRank = ThreadLocalRandom.current().nextInt(1, 300);
        }

        return contextoService.getHint(session.getGameId(), targetRank)
                .map(response -> {
                    if (response != null && response.word() != null && response.distance() != null) {
                        int normalizedDistance = response.distance() + 1;
                        ContextoGuess hintGuess = new ContextoGuess(response.word(), normalizedDistance, Instant.now(), true);
                        session.addGuess(hintGuess);
                        session.setStatusMessage(String.format("💡 Hint revealed: **%s** is #%d!", response.word(), normalizedDistance));
                        persistSession(session);
                    } else {
                        session.setStatusMessage("Could not retrieve a hint right now. Try guessing a word!");
                    }
                    return session;
                })
                .toFuture();
    }

    public CompletableFuture<ContextoSession> giveUp(String userId) {
        ContextoSession session = sessions.getIfPresent(userId);
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (session.isFinished()) {
            return CompletableFuture.completedFuture(session);
        }

        return contextoService.giveUp(session.getGameId())
                .map(response -> {
                    String secret = response != null && response.word() != null ? response.word() : "Unknown";
                    session.giveUp(secret);
                    session.setStatusMessage(String.format("🏳️ You gave up! The secret word was **%s**.", secret));
                    persistSession(session);
                    return session;
                })
                .toFuture();
    }

    public synchronized void resetSession(String userId) {
        sessions.invalidate(userId);
        if (jdbcClient != null) {
            try {
                int daily = getDailyGameId();
                jdbcClient.sql("DELETE FROM contexto_sessions WHERE user_id = ? AND game_id = ?")
                        .params(userId, daily)
                        .update();
            } catch (Exception e) {
                log.error("Failed to delete session {} from database: {}", userId, e.getMessage());
            }
        }
        saveSessionsToFile();
        log.info("Reset and invalidated Contexto session for user {}", userId);
    }
}
