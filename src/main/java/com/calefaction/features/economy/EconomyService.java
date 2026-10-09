package com.calefaction.features.economy;

import com.calefaction.features.economy.model.UserWallet;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
public class EconomyService {

    private static final Logger log = LoggerFactory.getLogger(EconomyService.class);
    public static final long STARTING_BALANCE = 1000L;
    public static final long BASE_DAILY_REWARD = 1000L;
    public static final long STREAK_BONUS = 100L;
    public static final long MAX_DAILY_REWARD = 2500L;
    public static final long BAILOUT_AMOUNT = 1000L;
    public static final long BAILOUT_THRESHOLD = 100L;

    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;
    private final Map<String, UserWallet> wallets = new ConcurrentHashMap<>();
    private Path storageFile;

    @Autowired
    public EconomyService(
            @Autowired(required = false) JdbcClient jdbcClient,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
    }

    public EconomyService(ObjectMapper objectMapper, Path storageFile) {
        this.jdbcClient = null;
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
        this.storageFile = storageFile;
    }

    public EconomyService(ObjectMapper objectMapper) {
        this((JdbcClient) null, objectMapper);
    }

    public EconomyService() {
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
                this.storageFile = baseDir.resolve("economy.json");
            } catch (Exception e) {
                log.warn("Failed to initialize economy storage directory: {}", e.getMessage());
                this.storageFile = Path.of("economy.json");
            }
        }
        loadFromFile();
    }

    private synchronized void loadFromDatabase() {
        if (jdbcClient == null) {
            return;
        }
        try {
            List<UserWallet> loaded = jdbcClient.sql("SELECT user_id, balance, total_won, total_bet, spins_played, last_daily_claim, daily_streak FROM economy_wallets")
                    .query((rs, rowNum) -> {
                        Timestamp ts = rs.getTimestamp("last_daily_claim");
                        return new UserWallet(
                                rs.getString("user_id"),
                                rs.getLong("balance"),
                                rs.getLong("total_won"),
                                rs.getLong("total_bet"),
                                rs.getInt("spins_played"),
                                ts != null ? ts.toInstant() : null,
                                rs.getInt("daily_streak")
                        );
                    }).list();
            for (UserWallet w : loaded) {
                wallets.put(w.userId(), w);
            }
            log.info("Loaded {} user wallets from database", wallets.size());
        } catch (Exception e) {
            log.debug("Could not load wallets from database: {}", e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        if (storageFile != null && Files.exists(storageFile)) {
            try {
                Map<String, UserWallet> loaded = objectMapper.readValue(
                        storageFile,
                        new TypeReference<>() {}
                );
                if (loaded != null) {
                    wallets.putAll(loaded);
                    log.info("Loaded {} user wallets from {}", wallets.size(), storageFile);
                }
            } catch (Exception e) {
                log.error("Failed to load economy data from {}: {}", storageFile, e.getMessage());
            }
        }
    }

    private synchronized void saveToFile() {
        if (storageFile == null) {
            return;
        }
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmpFile = storageFile.resolveSibling(storageFile.getFileName().toString() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpFile, wallets);
            Files.move(tmpFile, storageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.error("Failed to save economy data to {}: {}", storageFile, e.getMessage());
        }
    }

    public synchronized void persistWallet(UserWallet wallet) {
        wallets.put(wallet.userId(), wallet);
        if (jdbcClient != null) {
            try {
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
                        wallet.userId(),
                        wallet.balance(),
                        wallet.totalWon(),
                        wallet.totalBet(),
                        wallet.spinsPlayed(),
                        wallet.lastDailyClaim() != null ? Timestamp.from(wallet.lastDailyClaim()) : null,
                        wallet.dailyStreak()
                ).update();
            } catch (Exception e) {
                log.error("Failed to persist wallet {} to database: {}", wallet.userId(), e.getMessage());
            }
        }
        saveToFile();
    }

    public UserWallet getWallet(String userId) {
        UserWallet existing = wallets.get(userId);
        if (existing != null) {
            return existing;
        }
        if (jdbcClient != null) {
            try {
                var found = jdbcClient.sql("SELECT user_id, balance, total_won, total_bet, spins_played, last_daily_claim, daily_streak FROM economy_wallets WHERE user_id = ?")
                        .param(userId)
                        .query((rs, rowNum) -> {
                            Timestamp ts = rs.getTimestamp("last_daily_claim");
                            return new UserWallet(
                                    rs.getString("user_id"),
                                    rs.getLong("balance"),
                                    rs.getLong("total_won"),
                                    rs.getLong("total_bet"),
                                    rs.getInt("spins_played"),
                                    ts != null ? ts.toInstant() : null,
                                    rs.getInt("daily_streak")
                            );
                        }).optional();
                if (found.isPresent()) {
                    wallets.put(userId, found.get());
                    return found.get();
                }
            } catch (Exception e) {
                log.debug("Database lookup for wallet {}: {}", userId, e.getMessage());
            }
        }
        UserWallet def = UserWallet.createDefault(userId);
        persistWallet(def);
        return def;
    }

    public long getBalance(String userId) {
        return getWallet(userId).balance();
    }

    public synchronized boolean deductCoins(String userId, long amount) {
        if (amount <= 0) return true;
        UserWallet current = getWallet(userId);
        if (current.balance() < amount) {
            return false;
        }
        persistWallet(current.withBalance(current.balance() - amount));
        return true;
    }

    public synchronized void addCoins(String userId, long amount) {
        if (amount <= 0) return;
        UserWallet current = getWallet(userId);
        persistWallet(current.withBalance(current.balance() + amount));
    }

    public synchronized void recordGameResult(String userId, long betAmount, long winAmount) {
        UserWallet current = getWallet(userId);
        // Only update stats — balance is already handled by deductCoins/addCoins
        UserWallet updated = new UserWallet(
                userId,
                current.balance(),
                current.totalWon() + winAmount,
                current.totalBet() + betAmount,
                current.spinsPlayed() + 1,
                current.lastDailyClaim(),
                current.dailyStreak()
        );
        persistWallet(updated);
    }

    public record DailyResult(boolean success, long amountClaimed, int streak, String message, Duration timeUntilNext) {}

    public synchronized DailyResult claimDaily(String userId) {
        UserWallet current = getWallet(userId);
        Instant now = Instant.now();
        Instant last = current.lastDailyClaim();

        if (last != null) {
            Duration elapsed = Duration.between(last, now);
            if (elapsed.toHours() < 20) { // 20 hours cooldown
                Duration remaining = Duration.ofHours(24).minus(elapsed);
                if (remaining.isNegative()) remaining = Duration.ZERO;
                return new DailyResult(false, 0, current.dailyStreak(),
                        "⏳ You already claimed your daily reward recently!", remaining);
            }
        }

        int newStreak = 1;
        if (last != null) {
            Duration elapsed = Duration.between(last, now);
            if (elapsed.toHours() <= 48) {
                newStreak = current.dailyStreak() + 1;
            }
        }

        long reward = Math.min(BASE_DAILY_REWARD + ((long) (newStreak - 1) * STREAK_BONUS), MAX_DAILY_REWARD);
        persistWallet(current.withDaily(reward, now, newStreak));

        return new DailyResult(true, reward, newStreak, "🎉 Claimed " + reward + " coins!", Duration.ZERO);
    }

    public record BailoutResult(boolean success, long amountGranted, String message) {}

    public synchronized BailoutResult claimBailout(String userId) {
        UserWallet current = getWallet(userId);
        if (current.balance() >= BAILOUT_THRESHOLD) {
            return new BailoutResult(false, 0,
                    "❌ You have **" + current.balance() + " 🪙**! Emergency bailout is only available when your balance is under **" + BAILOUT_THRESHOLD + " 🪙**.");
        }

        persistWallet(current.withBailout(BAILOUT_AMOUNT));
        return new BailoutResult(true, BAILOUT_AMOUNT,
                "🚨 **Emergency Bailout Approved!** Granted **" + BAILOUT_AMOUNT + " coins** to get you back in the game! 🪙");
    }

    public List<UserWallet> getTopWallets(int limit) {
        return wallets.values().stream()
                .sorted(Comparator.comparingLong(UserWallet::balance).reversed())
                .limit(limit)
                .toList();
    }
}
