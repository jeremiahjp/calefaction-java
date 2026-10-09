package com.calefaction.features.games.contexto.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ContextoSession {

    private final String userId;
    private final int gameId;
    private final boolean daily;
    private final List<ContextoGuess> guesses = new ArrayList<>();
    private final Instant createdAt = Instant.now();
    private Instant lastActiveAt = Instant.now();

    private boolean won = false;
    private boolean givenUp = false;
    private boolean announcedVictory = false;
    private boolean announcedStart = false;
    private final Set<String> sharedChannelIds = ConcurrentHashMap.newKeySet();
    private String channelId = null;
    private String guildId = null;
    private String secretWord = null;
    private ContextoGuess lastGuess = null;
    private String statusMessage = null;
    private int restoredTotalGuesses = 0;
    private int restoredHintsUsed = 0;

    public ContextoSession(String userId, int gameId, boolean daily) {
        this.userId = userId;
        this.gameId = gameId;
        this.daily = daily;
    }

    public synchronized boolean hasGuessed(String word) {
        return guesses.stream().anyMatch(g -> g.word().equalsIgnoreCase(word.trim()));
    }

    public synchronized void addGuess(ContextoGuess guess) {
        guesses.add(guess);
        this.lastGuess = guess;
        this.lastActiveAt = Instant.now();
        this.statusMessage = null;
        if (guess.distance() == 0 || guess.distance() == 1) {
            this.won = true;
            this.secretWord = guess.word();
        }
    }

    public synchronized void giveUp(String secretWord) {
        this.givenUp = true;
        this.secretWord = secretWord;
        this.lastActiveAt = Instant.now();
    }

    public synchronized boolean isFinished() {
        return won || givenUp;
    }

    public synchronized List<ContextoGuess> getClosestGuesses(int limit) {
        return guesses.stream()
                .sorted(Comparator.comparingInt(ContextoGuess::distance))
                .limit(limit)
                .toList();
    }

    public synchronized OptionalInt getBestDistance() {
        return guesses.stream().mapToInt(ContextoGuess::distance).min();
    }

    public synchronized void restoreWin(int totalGuesses, int hintsUsed, Instant completedAt) {
        this.won = true;
        this.restoredTotalGuesses = totalGuesses;
        this.restoredHintsUsed = hintsUsed;
        if (completedAt != null) {
            this.lastActiveAt = completedAt;
        }
    }

    public synchronized int getHintsUsed() {
        int guessHints = (int) guesses.stream().filter(ContextoGuess::isHint).count();
        return Math.max(restoredHintsUsed, guessHints);
    }

    public synchronized int getTotalGuesses() {
        return Math.max(restoredTotalGuesses, guesses.size());
    }

    public synchronized int getGreenCount() {
        int count = (int) guesses.stream().filter(g -> g.distance() <= 300).count();
        return (count == 0 && won && guesses.isEmpty()) ? 1 : count;
    }

    public synchronized int getYellowCount() {
        return (int) guesses.stream().filter(g -> g.distance() > 300 && g.distance() <= 1500).count();
    }

    public synchronized int getRedCount() {
        return (int) guesses.stream().filter(g -> g.distance() > 1500).count();
    }

    public String getUserId() {
        return userId;
    }

    public int getGameId() {
        return gameId;
    }

    public boolean isDaily() {
        return daily;
    }

    public synchronized List<ContextoGuess> getGuesses() {
        return new ArrayList<>(guesses);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public synchronized boolean isWon() {
        return won;
    }

    public synchronized boolean isGivenUp() {
        return givenUp;
    }

    public synchronized String getSecretWord() {
        return secretWord;
    }

    public synchronized ContextoGuess getLastGuess() {
        return lastGuess;
    }

    public synchronized String getStatusMessage() {
        return statusMessage;
    }

    public synchronized void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        this.lastActiveAt = Instant.now();
    }

    public synchronized boolean isAnnouncedVictory() {
        return announcedVictory;
    }

    public synchronized void setAnnouncedVictory(boolean announcedVictory) {
        this.announcedVictory = announcedVictory;
    }

    public synchronized boolean isAnnouncedStart() {
        return announcedStart;
    }

    public synchronized void setAnnouncedStart(boolean announcedStart) {
        this.announcedStart = announcedStart;
    }

    public synchronized boolean isShared() {
        return !sharedChannelIds.isEmpty();
    }

    public synchronized boolean hasSharedToChannel(String channelId) {
        return channelId != null && sharedChannelIds.contains(channelId);
    }

    public synchronized boolean markSharedToChannel(String channelId) {
        if (channelId != null) {
            return sharedChannelIds.add(channelId);
        }
        return false;
    }

    public synchronized Set<String> getSharedChannelIds() {
        return Set.copyOf(sharedChannelIds);
    }

    public synchronized String getChannelId() {
        return channelId;
    }

    public synchronized void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public synchronized String getGuildId() {
        return guildId;
    }

    public synchronized void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public synchronized ContextoSessionData toData() {
        return new ContextoSessionData(
                userId,
                gameId,
                daily,
                new ArrayList<>(guesses),
                createdAt,
                lastActiveAt,
                won,
                givenUp,
                announcedVictory,
                announcedStart,
                Set.copyOf(sharedChannelIds),
                channelId,
                guildId,
                secretWord,
                restoredTotalGuesses,
                restoredHintsUsed
        );
    }

    public static ContextoSession fromData(ContextoSessionData data) {
        ContextoSession session = new ContextoSession(data.userId(), data.gameId(), data.daily());
        if (data.guesses() != null) {
            for (ContextoGuess g : data.guesses()) {
                if (g.distance() == 0) {
                    session.guesses.add(new ContextoGuess(g.word(), 1, g.timestamp(), g.isHint()));
                } else {
                    session.guesses.add(g);
                }
            }
            if (!session.guesses.isEmpty()) {
                session.lastGuess = session.guesses.get(session.guesses.size() - 1);
            }
        }
        session.won = data.won();
        session.givenUp = data.givenUp();
        session.announcedVictory = data.announcedVictory();
        session.announcedStart = data.announcedStart();
        if (data.sharedChannelIds() != null) {
            session.sharedChannelIds.addAll(data.sharedChannelIds());
        }
        session.channelId = data.channelId();
        session.guildId = data.guildId();
        session.secretWord = data.secretWord();
        session.restoredTotalGuesses = data.restoredTotalGuesses();
        session.restoredHintsUsed = data.restoredHintsUsed();
        if (data.lastActiveAt() != null) {
            session.lastActiveAt = data.lastActiveAt();
        }
        return session;
    }
}
