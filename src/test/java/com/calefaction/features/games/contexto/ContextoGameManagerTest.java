package com.calefaction.features.games.contexto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.calefaction.features.games.contexto.dto.ContextoGiveUpResponse;
import com.calefaction.features.games.contexto.dto.ContextoGuessResponse;
import com.calefaction.features.games.contexto.dto.ContextoTipResponse;
import com.calefaction.features.games.contexto.model.ContextoSession;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;

class ContextoGameManagerTest {

    @TempDir
    Path tempDir;

    private ContextoService mockService;
    private ContextoGameManager gameManager;

    @BeforeEach
    void setUp() {
        mockService = mock(ContextoService.class);
        when(mockService.getDailyGameId()).thenReturn(1458);
        gameManager = new ContextoGameManager(mockService);
        gameManager.setSessionsStorageFile(tempDir.resolve("contexto-sessions.json"));
    }

    @Test
    void testStartDailyGame() {
        ContextoSession session = gameManager.startDailyGame("user-1");
        assertNotNull(session);
        assertEquals(1458, session.getGameId());
        assertTrue(session.isDaily());
        assertEquals("user-1", session.getUserId());
        assertFalse(session.isFinished());
    }

    @Test
    void testSubmitGuessAndWin() {
        gameManager.startDailyGame("user-1");

        when(mockService.guess(1458, "hat"))
                .thenReturn(Mono.just(new ContextoGuessResponse(10, "hat", "hat", null)));
        when(mockService.guess(1458, "bandana"))
                .thenReturn(Mono.just(new ContextoGuessResponse(0, "bandana", "bandana", null)));

        CompletableFuture<ContextoSession> f1 = gameManager.submitGuess("user-1", "hat");
        ContextoSession s1 = f1.join();
        assertNotNull(s1);
        assertEquals(1, s1.getTotalGuesses());
        assertEquals(11, s1.getBestDistance().orElse(-1));
        assertFalse(s1.isWon());

        CompletableFuture<ContextoSession> f2 = gameManager.submitGuess("user-1", "bandana");
        ContextoSession s2 = f2.join();
        assertNotNull(s2);
        assertEquals(2, s2.getTotalGuesses());
        assertTrue(s2.isWon());
        assertEquals(1, s2.getBestDistance().orElse(-1));
        assertEquals("bandana", s2.getSecretWord());
    }

    @Test
    void testDuplicateGuessRejection() {
        gameManager.startDailyGame("user-1");
        when(mockService.guess(1458, "hat"))
                .thenReturn(Mono.just(new ContextoGuessResponse(10, "hat", "hat", null)));

        gameManager.submitGuess("user-1", "hat").join();
        ContextoSession s = gameManager.submitGuess("user-1", "hat").join();

        assertEquals(1, s.getTotalGuesses());
        assertTrue(s.getStatusMessage().contains("already guessed"));
    }

    @Test
    void testHint() {
        gameManager.startDailyGame("user-1");
        when(mockService.guess(1458, "hat"))
                .thenReturn(Mono.just(new ContextoGuessResponse(10, "hat", "hat", null)));
        when(mockService.getHint(eq(1458), anyInt()))
                .thenReturn(Mono.just(new ContextoTipResponse(5, "cap", "cap")));

        gameManager.submitGuess("user-1", "hat").join();
        ContextoSession s = gameManager.requestHint("user-1").join();

        assertEquals(2, s.getTotalGuesses());
        assertEquals(1, s.getHintsUsed());
        assertEquals(6, s.getBestDistance().orElse(-1));
    }

    @Test
    void testGiveUp() {
        gameManager.startDailyGame("user-1");
        when(mockService.giveUp(1458))
                .thenReturn(Mono.just(new ContextoGiveUpResponse(0, "bandana", "bandana")));

        ContextoSession s = gameManager.giveUp("user-1").join();
        assertTrue(s.isGivenUp());
        assertTrue(s.isFinished());
        assertEquals("bandana", s.getSecretWord());
    }

    @Test
    void testSessionPersistenceAcrossRestarts() {
        gameManager.startDailyGame("user-persistence");
        when(mockService.guess(1458, "forest"))
                .thenReturn(Mono.just(new ContextoGuessResponse(35, "forest", "forest", null)));
        gameManager.submitGuess("user-persistence", "forest").join();

        // Simulate new manager after restart loading the same file
        ContextoGameManager newManager = new ContextoGameManager(mockService);
        newManager.setSessionsStorageFile(tempDir.resolve("contexto-sessions.json"));

        var sessionOpt = newManager.getSession("user-persistence");
        assertTrue(sessionOpt.isPresent());
        ContextoSession loaded = sessionOpt.get();
        assertEquals(1, loaded.getTotalGuesses());
        assertEquals("forest", loaded.getLastGuess().word());
        assertEquals(36, loaded.getLastGuess().distance());
    }

    @Test
    void testResetSession() {
        gameManager.startDailyGame("user-reset");
        assertTrue(gameManager.getSession("user-reset").isPresent());

        gameManager.resetSession("user-reset");
        assertTrue(gameManager.getSession("user-reset").isEmpty());

        // Verify it was persisted as removed
        ContextoGameManager reloaded = new ContextoGameManager(mockService);
        reloaded.setSessionsStorageFile(tempDir.resolve("contexto-sessions.json"));
        assertTrue(reloaded.getSession("user-reset").isEmpty());
    }
}
