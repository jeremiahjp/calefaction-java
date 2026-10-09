package com.calefaction.features.games.contexto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class ContextoLeaderboardServiceTest {

    @TempDir
    Path tempDir;

    private ContextoLeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = new ContextoLeaderboardService(
                JsonMapper.builder().build(),
                tempDir.resolve("contexto-leaderboard.json")
        );
        leaderboardService.init();
    }

    @Test
    void testRecordWinAndOrdering() {
        leaderboardService.recordWin("guild-1", 1458, "user-bob", 45, 1);
        leaderboardService.recordWin("guild-1", 1458, "user-alice", 12, 0);
        leaderboardService.recordWin("guild-1", 1458, "user-charlie", 25, 2);

        List<ContextoLeaderboardService.LeaderboardEntry> entries = leaderboardService.getLeaderboard("guild-1", 1458);
        assertEquals(3, entries.size());

        // Alice is 1st with 12 guesses
        assertEquals("user-alice", entries.get(0).userId());
        assertEquals(12, entries.get(0).guesses());

        // Charlie is 2nd with 25 guesses
        assertEquals("user-charlie", entries.get(1).userId());

        // Bob is 3rd with 45 guesses
        assertEquals("user-bob", entries.get(2).userId());
    }

    @Test
    void testKeepBestScore() {
        leaderboardService.recordWin("guild-1", 1458, "user-1", 30, 0);
        // Worse score should be ignored
        leaderboardService.recordWin("guild-1", 1458, "user-1", 50, 0);

        List<ContextoLeaderboardService.LeaderboardEntry> entries = leaderboardService.getLeaderboard("guild-1", 1458);
        assertEquals(1, entries.size());
        assertEquals(30, entries.get(0).guesses());

        // Better score should replace
        leaderboardService.recordWin("guild-1", 1458, "user-1", 15, 0);
        entries = leaderboardService.getLeaderboard("guild-1", 1458);
        assertEquals(1, entries.size());
        assertEquals(15, entries.get(0).guesses());
    }

    @Test
    void testEmptyLeaderboard() {
        List<ContextoLeaderboardService.LeaderboardEntry> entries = leaderboardService.getLeaderboard("guild-unknown", 999);
        assertTrue(entries.isEmpty());
    }

    @Test
    void testHasUserWonAndGetUserEntry() {
        assertTrue(leaderboardService.getUserEntry("guild-1", 1458, "user-alice").isEmpty());
        assertEquals(false, leaderboardService.hasUserWon("guild-1", 1458, "user-alice"));

        leaderboardService.recordWin("guild-1", 1458, "user-alice", 18, 0);

        assertTrue(leaderboardService.hasUserWon("guild-1", 1458, "user-alice"));
        assertTrue(leaderboardService.getUserEntry("guild-1", 1458, "user-alice").isPresent());
        assertEquals(18, leaderboardService.getUserEntry("guild-1", 1458, "user-alice").get().guesses());
    }

    @Test
    void testFindAnyUserEntry() {
        assertTrue(leaderboardService.findAnyUserEntry(1458, "user-cross").isEmpty());

        leaderboardService.recordWin("guild-alpha", 1458, "user-cross", 22, 1);

        var found = leaderboardService.findAnyUserEntry(1458, "user-cross");
        assertTrue(found.isPresent());
        assertEquals(22, found.get().guesses());
        assertEquals(1, found.get().hints());
    }

    @Test
    void testRemoveUserWin() {
        leaderboardService.recordWin("guild-1", 1458, "user-remove", 15, 0);
        leaderboardService.recordWin("guild-2", 1458, "user-remove", 12, 1);
        assertTrue(leaderboardService.hasUserWon("guild-1", 1458, "user-remove"));

        boolean removed = leaderboardService.removeUserWin(1458, "user-remove");
        assertTrue(removed);
        assertFalse(leaderboardService.hasUserWon("guild-1", 1458, "user-remove"));
        assertFalse(leaderboardService.hasUserWon("guild-2", 1458, "user-remove"));
        assertTrue(leaderboardService.findAnyUserEntry(1458, "user-remove").isEmpty());

        // Removing non-existent returns false
        assertFalse(leaderboardService.removeUserWin(1458, "user-remove"));
    }
}
