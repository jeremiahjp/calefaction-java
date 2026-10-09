package com.calefaction.features.games.contexto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class ContextoServiceTest {

    @Test
    void testDailyGameIdIsPositiveAndReasonable() {
        ContextoService service = new ContextoService(WebClient.builder());
        int dailyGameId = service.getDailyGameId();

        // 2022-09-18 was day 0, today in 2026 should be > 1000
        assertTrue(dailyGameId > 1000);
        assertEquals(ZoneId.of("America/Chicago"), service.getZoneId());
    }

    @Test
    void testCustomTimezone() {
        ContextoService service = new ContextoService(WebClient.builder(), "America/New_York");
        assertEquals(ZoneId.of("America/New_York"), service.getZoneId());
    }

    @Test
    void testInvalidTimezoneFallback() {
        ContextoService service = new ContextoService(WebClient.builder(), "Not/A_Valid_Zone");
        assertEquals(ZoneId.of("America/Chicago"), service.getZoneId());
    }

    @Test
    void testSanitizeWord() {
        assertEquals("apple", ContextoService.sanitizeWord("  Apple! "));
        assertEquals("coffee", ContextoService.sanitizeWord("COFFEE123"));
        assertEquals("icecream", ContextoService.sanitizeWord("ice-cream"));
        assertEquals("", ContextoService.sanitizeWord(null));
        assertEquals("", ContextoService.sanitizeWord("   "));
    }
}
