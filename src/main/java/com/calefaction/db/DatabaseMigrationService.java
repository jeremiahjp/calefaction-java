package com.calefaction.db;

import com.calefaction.features.economy.model.UserWallet;
import com.calefaction.features.games.contexto.ContextoLeaderboardService.LeaderboardEntry;
import com.calefaction.features.games.contexto.model.ContextoSessionData;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
@Order(1)
public class DatabaseMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public DatabaseMigrationService(JdbcClient jdbcClient, @Autowired(required = false) ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    @PostConstruct
    public void init() {
        try {
            initSchema();
            migrateLegacyJsonFiles();
        } catch (Exception e) {
            log.error("Failed during database schema initialization or migration: {}", e.getMessage(), e);
        }
    }

    public void initSchema() {
        log.info("Initializing database tables and indexes if not exists...");

        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS economy_wallets (
                user_id VARCHAR(64) PRIMARY KEY,
                balance BIGINT NOT NULL DEFAULT 1000,
                total_won BIGINT NOT NULL DEFAULT 0,
                total_bet BIGINT NOT NULL DEFAULT 0,
                spins_played INT NOT NULL DEFAULT 0,
                last_daily_claim TIMESTAMP WITH TIME ZONE,
                daily_streak INT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
            )
        """).update();

        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS contexto_sessions (
                user_id VARCHAR(64) NOT NULL,
                game_id INT NOT NULL,
                is_won BOOLEAN NOT NULL DEFAULT FALSE,
                is_given_up BOOLEAN NOT NULL DEFAULT FALSE,
                is_daily BOOLEAN NOT NULL DEFAULT TRUE,
                data_json TEXT NOT NULL,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                CONSTRAINT pk_contexto_sessions PRIMARY KEY (user_id, game_id)
            )
        """).update();

        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS contexto_leaderboard (
                id BIGSERIAL PRIMARY KEY,
                guild_id VARCHAR(64) NOT NULL,
                game_id INT NOT NULL,
                user_id VARCHAR(64) NOT NULL,
                guesses INT NOT NULL,
                hints INT NOT NULL,
                is_daily BOOLEAN NOT NULL DEFAULT TRUE,
                completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                CONSTRAINT uq_contexto_lb UNIQUE (guild_id, game_id, user_id)
            )
        """).update();

        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS contexto_starts (
                start_key VARCHAR(128) PRIMARY KEY,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
            )
        """).update();

        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS drop_tweet_notifications (
                tweet_id VARCHAR(64) PRIMARY KEY,
                account VARCHAR(64) NOT NULL,
                tweet_text TEXT NOT NULL,
                matched_keyword VARCHAR(128),
                created_at TIMESTAMP WITH TIME ZONE,
                notified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
            )
        """).update();

        try {
            jdbcClient.sql("CREATE INDEX IF NOT EXISTS idx_economy_balance ON economy_wallets(balance DESC)").update();
            jdbcClient.sql("CREATE INDEX IF NOT EXISTS idx_contexto_sessions_game ON contexto_sessions(game_id)").update();
            jdbcClient.sql("CREATE INDEX IF NOT EXISTS idx_contexto_lb_lookup ON contexto_leaderboard(guild_id, game_id)").update();
            jdbcClient.sql("CREATE INDEX IF NOT EXISTS idx_drop_tweets_account ON drop_tweet_notifications (account, notified_at DESC)").update();
        } catch (Exception e) {
            log.debug("Index creation note: {}", e.getMessage());
        }

        log.info("Database schema is initialized.");
    }

    public void migrateLegacyJsonFiles() {
        Path[] candidateDirs = new Path[] {
                Path.of("/data"),
                Path.of("data"),
                Path.of(".")
        };

        for (Path dir : candidateDirs) {
            if (!Files.exists(dir)) continue;

            migrateEconomy(dir.resolve("economy.json"));
            migrateSessions(dir.resolve("contexto-sessions.json"));
            migrateLeaderboard(dir.resolve("contexto-leaderboard.json"));
            migrateStarts(dir.resolve("contexto-starts.json"));
        }
    }

    private void migrateEconomy(Path file) {
        if (!Files.exists(file) || Files.isDirectory(file)) return;
        try {
            Map<String, UserWallet> loaded = objectMapper.readValue(file, new TypeReference<>() {});
            if (loaded != null && !loaded.isEmpty()) {
                int count = 0;
                for (UserWallet w : loaded.values()) {
                    jdbcClient.sql("""
                        INSERT INTO economy_wallets (user_id, balance, total_won, total_bet, spins_played, last_daily_claim, daily_streak, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (user_id) DO UPDATE SET
                            balance = EXCLUDED.balance,
                            total_won = EXCLUDED.total_won,
                            total_bet = EXCLUDED.total_bet,
                            spins_played = EXCLUDED.spins_played,
                            last_daily_claim = EXCLUDED.last_daily_claim,
                            daily_streak = EXCLUDED.daily_streak,
                            updated_at = NOW()
                    """)
                    .params(
                            w.userId(),
                            w.balance(),
                            w.totalWon(),
                            w.totalBet(),
                            w.spinsPlayed(),
                            w.lastDailyClaim() != null ? Timestamp.from(w.lastDailyClaim()) : null,
                            w.dailyStreak()
                    ).update();
                    count++;
                }
                log.info("Migrated {} wallets from {} into PostgreSQL", count, file);
                archiveFile(file);
            }
        } catch (Exception e) {
            log.warn("Error migrating economy file {}: {}", file, e.getMessage());
        }
    }

    private void migrateSessions(Path file) {
        if (!Files.exists(file) || Files.isDirectory(file)) return;
        try {
            Map<String, ContextoSessionData> loaded = objectMapper.readValue(file, new TypeReference<>() {});
            if (loaded != null && !loaded.isEmpty()) {
                int count = 0;
                for (ContextoSessionData s : loaded.values()) {
                    String json = objectMapper.writeValueAsString(s);
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
                    .params(s.userId(), s.gameId(), s.won(), s.givenUp(), s.daily(), json)
                    .update();
                    count++;
                }
                log.info("Migrated {} Contexto sessions from {} into PostgreSQL", count, file);
                archiveFile(file);
            }
        } catch (Exception e) {
            log.warn("Error migrating sessions file {}: {}", file, e.getMessage());
        }
    }

    private void migrateLeaderboard(Path file) {
        if (!Files.exists(file) || Files.isDirectory(file)) return;
        try {
            Map<String, Map<Integer, List<LeaderboardEntry>>> loaded = objectMapper.readValue(file, new TypeReference<>() {});
            if (loaded != null && !loaded.isEmpty()) {
                int count = 0;
                for (var guildEntry : loaded.entrySet()) {
                    String guildId = guildEntry.getKey();
                    for (var gameEntry : guildEntry.getValue().entrySet()) {
                        int gameId = gameEntry.getKey();
                        for (LeaderboardEntry entry : gameEntry.getValue()) {
                            jdbcClient.sql("""
                                INSERT INTO contexto_leaderboard (guild_id, game_id, user_id, guesses, hints, is_daily, completed_at)
                                VALUES (?, ?, ?, ?, ?, ?, ?)
                                ON CONFLICT (guild_id, game_id, user_id) DO UPDATE SET
                                    guesses = EXCLUDED.guesses,
                                    hints = EXCLUDED.hints,
                                    is_daily = EXCLUDED.is_daily,
                                    completed_at = EXCLUDED.completed_at
                            """)
                            .params(
                                    guildId,
                                    gameId,
                                    entry.userId(),
                                    entry.guesses(),
                                    entry.hints(),
                                    true,
                                    Timestamp.from(entry.completedAt())
                            ).update();
                            count++;
                        }
                    }
                }
                log.info("Migrated {} Contexto leaderboard entries from {} into PostgreSQL", count, file);
                archiveFile(file);
            }
        } catch (Exception e) {
            log.warn("Error migrating leaderboard file {}: {}", file, e.getMessage());
        }
    }

    private void migrateStarts(Path file) {
        if (!Files.exists(file) || Files.isDirectory(file)) return;
        try {
            Set<String> loaded = objectMapper.readValue(file, new TypeReference<>() {});
            if (loaded != null && !loaded.isEmpty()) {
                int count = 0;
                for (String key : loaded) {
                    jdbcClient.sql("""
                        INSERT INTO contexto_starts (start_key, created_at)
                        VALUES (?, NOW())
                        ON CONFLICT (start_key) DO NOTHING
                    """)
                    .params(key)
                    .update();
                    count++;
                }
                log.info("Migrated {} Contexto starts from {} into PostgreSQL", count, file);
                archiveFile(file);
            }
        } catch (Exception e) {
            log.warn("Error migrating starts file {}: {}", file, e.getMessage());
        }
    }

    private void archiveFile(Path file) {
        try {
            Path target = file.resolveSibling(file.getFileName().toString() + ".migrated");
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archived migrated file {} -> {}", file.getFileName(), target.getFileName());
        } catch (Exception e) {
            log.warn("Could not rename migrated file {}: {}", file, e.getMessage());
        }
    }
}
