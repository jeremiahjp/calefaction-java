package com.calefaction.features.games.contexto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.calefaction.features.games.contexto.dto.ContextoGiveUpResponse;
import com.calefaction.features.games.contexto.dto.ContextoGuessResponse;
import com.calefaction.features.games.contexto.dto.ContextoTipResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class ContextoServiceLiveTest {

    @Test
    void testLiveContextoApiEndpoints() {
        ContextoService service = new ContextoService(WebClient.builder());
        int gameId = service.getDailyGameId();

        // 1. Test guess endpoint
        ContextoGuessResponse guess = service.guess(gameId, "hat")
                .block(Duration.ofSeconds(5));
        assertNotNull(guess);
        assertFalse(guess.isError());
        assertNotNull(guess.distance());
        assertEquals("hat", guess.word());

        // 2. Test hint endpoint
        ContextoTipResponse hint = service.getHint(gameId, 100)
                .block(Duration.ofSeconds(5));
        assertNotNull(hint);
        assertNotNull(hint.word());

        // 3. Test give up endpoint
        ContextoGiveUpResponse giveUp = service.giveUp(gameId)
                .block(Duration.ofSeconds(5));
        assertNotNull(giveUp);
        assertNotNull(giveUp.word());
        assertTrue(giveUp.distance() == 0 || giveUp.distance() == 1);
    }
}
