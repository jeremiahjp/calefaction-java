package com.calefaction.features.games.contexto;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ContextoLeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(ContextoLeaderboardService.class);
    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;
    private final Map<String, Map<Integer, List<LeaderboardEntry>>> guildLeaderboards = new ConcurrentHashMap<>();
    private Path storageFile;

    public record LeaderboardEntry(
            String userId,
            int guesses,
            int hints,
            Instant completedAt
    ) {
    }

    private static final Comparator<LeaderboardEntry> RANK_COMPARATOR = Comparator
            .comparingInt(LeaderboardEntry::guesses)
            .thenComparingInt(LeaderboardEntry::hints)
            .thenComparing(LeaderboardEntry::completedAt);

    @Autowired
    public ContextoLeaderboardService(
            @Autowired(required = false) JdbcClient jdbcClient,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    public ContextoLeaderboardService(ObjectMapper objectMapper, Path storageFile) {
        this.jdbcClient = null;
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
        this.storageFile = storageFile;
    }

    public ContextoLeaderboardService(ObjectMapper objectMapper) {
        this((JdbcClient) null, objectMapper);
    }

    public ContextoLeaderboardService() {
        this((JdbcClient) null, null);
    }

    @PostConstruct
    public void init() {
        loadFromDatabase();
        if (storageFile == null) {
            Path haDataDir = Path.of("/data");
            Path baseDir = (Files.exists(haDataDir) && Files.isWritable(haDataDir)) ? haDataDir : Path.of("data");
            try {
                Files.createDirectories(baseDir);
                this.storageFile = baseDir.resolve("contexto-leaderboard.json");
            } catch (Exception e) {
                log.warn("Failed to initialize leaderboard storage directory: {}", e.getMessage());
                this.storageFile = Path.of("contexto-leaderboard.json");
            }
        }
        loadFromFile();
    }

    private synchronized void loadFromDatabase() {
        if (jdbcClient == null) {
            return;
        }
        try {
            var rows = jdbcClient.sql("SELECT guild_id, game_id, user_id, guesses, hints, completed_at FROM contexto_leaderboard")
                    .query((rs, rowNum) -> {
                        Timestamp ts = rs.getTimestamp("completed_at");
                        return new Object[] {
                                rs.getString("guild_id"),
                                rs.getInt("game_id"),
                                new LeaderboardEntry(
                                        rs.getString("user_id"),
                                        rs.getInt("guesses"),
                                        rs.getInt("hints"),
                                        ts != null ? ts.toInstant() : Instant.now()
                                )
                        };
                    }).list();

            int total = 0;
            for (Object[] row : rows) {
                String guildId = (String) row[0];
                int gameId = (Integer) row[1];
                LeaderboardEntry entry = (LeaderboardEntry) row[2];

                Map<Integer, List<LeaderboardEntry>> gameMap = guildLeaderboards.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>());
                List<LeaderboardEntry> entries = gameMap.computeIfAbsent(gameId, k -> new ArrayList<>());
                entries.add(entry);
                total++;
            }
            for (var gameMap : guildLeaderboards.values()) {
                for (var entries : gameMap.values()) {
                    entries.sort(RANK_COMPARATOR);
                }
            }
            log.info("Loaded {} Contexto leaderboard entries from database", total);
        } catch (Exception e) {
            log.debug("Could not load leaderboard from database: {}", e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        if (storageFile != null && Files.exists(storageFile)) {
            try {
                Map<String, Map<Integer, List<LeaderboardEntry>>> loaded = objectMapper.readValue(
                        storageFile,
                        new TypeReference<>() {}
                );
                if (loaded != null) {
                    guildLeaderboards.putAll(loaded);
                    int totalEntries = loaded.values().stream()
                            .mapToInt(m -> m.values().stream().mapToInt(List::size).sum())
                            .sum();
                    log.info("Loaded {} Contexto leaderboard entries from {}", totalEntries, storageFile);
                }
            } catch (Exception e) {
                log.error("Failed to load Contexto leaderboard from {}: {}", storageFile, e.getMessage());
            }
        }
    }

    private synchronized void saveToFile() {
        if (storageFile == null) {
            return;
        }
        try {
            Path tmpFile = Path.of(storageFile.toString() + ".tmp");
            objectMapper.writeValue(tmpFile, guildLeaderboards);
            try {
                Files.move(tmpFile, storageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicEx) {
                Files.move(tmpFile, storageFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("Failed to persist Contexto leaderboard to {}: {}", storageFile, e.getMessage());
        }
    }

    public synchronized void recordWin(String guildId, int gameId, String userId, int guesses, int hints) {
        String key = guildId != null ? guildId : "global";
        Map<Integer, List<LeaderboardEntry>> gameMap = guildLeaderboards.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        List<LeaderboardEntry> entries = gameMap.computeIfAbsent(gameId, k -> new ArrayList<>());

        // Check if user already has an entry
        LeaderboardEntry existing = entries.stream()
                .filter(e -> e.userId().equals(userId))
                .findFirst()
                .orElse(null);

        LeaderboardEntry newEntry = new LeaderboardEntry(userId, guesses, hints, Instant.now());
        if (existing == null) {
            entries.add(newEntry);
        } else if (guesses < existing.guesses() || (guesses == existing.guesses() && hints < existing.hints())) {
            entries.remove(existing);
            entries.add(newEntry);
        }

        entries.sort(RANK_COMPARATOR);
        if (jdbcClient != null) {
            try {
                jdbcClient.sql("""
                    INSERT INTO contexto_leaderboard (guild_id, game_id, user_id, guesses, hints, is_daily, completed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (guild_id, game_id, user_id) DO UPDATE SET
                        guesses = EXCLUDED.guesses,
                        hints = EXCLUDED.hints,
                        is_daily = EXCLUDED.is_daily,
                        completed_at = EXCLUDED.completed_at
                """)
                .params(key, gameId, userId, guesses, hints, true, Timestamp.from(newEntry.completedAt()))
                .update();
            } catch (Exception e) {
                log.error("Failed to persist leaderboard entry to database: {}", e.getMessage());
            }
        }
        saveToFile();
    }

    public synchronized List<LeaderboardEntry> getLeaderboard(String guildId, int gameId) {
        String key = guildId != null ? guildId : "global";
        Map<Integer, List<LeaderboardEntry>> gameMap = guildLeaderboards.get(key);
        if (gameMap == null || !gameMap.containsKey(gameId)) {
            return List.of();
        }
        return new ArrayList<>(gameMap.get(gameId));
    }

    public synchronized boolean hasUserWon(String guildId, int gameId, String userId) {
        String key = guildId != null ? guildId : "global";
        Map<Integer, List<LeaderboardEntry>> gameMap = guildLeaderboards.get(key);
        if (gameMap == null || !gameMap.containsKey(gameId)) {
            return false;
        }
        return gameMap.get(gameId).stream().anyMatch(e -> e.userId().equals(userId));
    }

    public synchronized Optional<LeaderboardEntry> getUserEntry(String guildId, int gameId, String userId) {
        String key = guildId != null ? guildId : "global";
        Map<Integer, List<LeaderboardEntry>> gameMap = guildLeaderboards.get(key);
        if (gameMap == null || !gameMap.containsKey(gameId)) {
            return Optional.empty();
        }
        return gameMap.get(gameId).stream()
                .filter(e -> e.userId().equals(userId))
                .findFirst();
    }

    public synchronized Optional<LeaderboardEntry> findAnyUserEntry(int gameId, String userId) {
        for (Map<Integer, List<LeaderboardEntry>> gameMap : guildLeaderboards.values()) {
            List<LeaderboardEntry> entries = gameMap.get(gameId);
            if (entries != null) {
                for (LeaderboardEntry e : entries) {
                    if (e.userId().equals(userId)) {
                        return Optional.of(e);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public synchronized boolean removeUserWin(int gameId, String userId) {
        boolean removed = false;
        for (Map<Integer, List<LeaderboardEntry>> gameMap : guildLeaderboards.values()) {
            List<LeaderboardEntry> entries = gameMap.get(gameId);
            if (entries != null) {
                if (entries.removeIf(e -> e.userId().equals(userId))) {
                    removed = true;
                }
            }
        }
        if (removed) {
            if (jdbcClient != null) {
                try {
                    jdbcClient.sql("DELETE FROM contexto_leaderboard WHERE game_id = ? AND user_id = ?")
                            .params(gameId, userId)
                            .update();
                } catch (Exception e) {
                    log.error("Failed to delete leaderboard entry from database: {}", e.getMessage());
                }
            }
            saveToFile();
            log.info("Removed Contexto #{} leaderboard entry for user {}", gameId, userId);
        }
        return removed;
    }
}
