package com.calefaction.features.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.calefaction.features.economy.model.UserWallet;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class EconomyServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testWalletDefaultsAndOperations() {
        Path file = tempDir.resolve("economy.json");
        EconomyService service = new EconomyService(JsonMapper.builder().build(), file);
        service.init();

        UserWallet wallet = service.getWallet("user-1");
        assertNotNull(wallet);
        assertEquals(EconomyService.STARTING_BALANCE, wallet.balance());

        // Deduct coins
        boolean deducted = service.deductCoins("user-1", 250);
        assertTrue(deducted);
        assertEquals(750, service.getBalance("user-1"));

        // Fail to deduct more than balance
        boolean overDeduct = service.deductCoins("user-1", 1000);
        assertFalse(overDeduct);
        assertEquals(750, service.getBalance("user-1"));

        // Add coins
        service.addCoins("user-1", 500);
        assertEquals(1250, service.getBalance("user-1"));
    }

    @Test
    void testDailyRewardAndCooldown() {
        Path file = tempDir.resolve("economy.json");
        EconomyService service = new EconomyService(JsonMapper.builder().build(), file);
        service.init();

        // First daily claim
        var result1 = service.claimDaily("user-daily");
        assertTrue(result1.success());
        assertEquals(EconomyService.BASE_DAILY_REWARD, result1.amountClaimed());
        assertEquals(1, result1.streak());
        assertEquals(2000, service.getBalance("user-daily"));

        // Attempt second claim immediately -> rejected
        var result2 = service.claimDaily("user-daily");
        assertFalse(result2.success());
        assertTrue(result2.timeUntilNext().toMinutes() > 0);
    }

    @Test
    void testEmergencyBailout() {
        Path file = tempDir.resolve("economy.json");
        EconomyService service = new EconomyService(JsonMapper.builder().build(), file);
        service.init();

        // User with 100 coins cannot claim bailout (threshold is strictly < 100)
        service.deductCoins("broke-user", 900); // 1000 - 900 = 100 coins
        assertEquals(100, service.getBalance("broke-user"));
        var failedBailout = service.claimBailout("broke-user");
        assertFalse(failedBailout.success());

        // Drain below 100 threshold
        service.deductCoins("broke-user", 10); // 100 - 10 = 90 coins (< 100)
        assertEquals(90, service.getBalance("broke-user"));

        // Claim bailout grants 1,000 coins
        var bailout = service.claimBailout("broke-user");
        assertTrue(bailout.success());
        assertEquals(1000L, bailout.amountGranted());
        assertEquals(90 + 1000L, service.getBalance("broke-user"));

        // Second bailout fails because balance is now >= 100 (1090 coins)
        var bailout2 = service.claimBailout("broke-user");
        assertFalse(bailout2.success());
    }

    @Test
    void testPersistenceAcrossRestarts() throws IOException {
        Path file = tempDir.resolve("economy.json");
        EconomyService service1 = new EconomyService(JsonMapper.builder().build(), file);
        service1.init();

        service1.deductCoins("player-x", 100);
        service1.addCoins("player-x", 500);
        service1.recordGameResult("player-x", 100, 500); // 1000 - 100 + 500 = 1400
        assertEquals(1400, service1.getBalance("player-x"));

        // Create new service instance pointing to same file
        EconomyService service2 = new EconomyService(JsonMapper.builder().build(), file);
        service2.init();

        assertEquals(1400, service2.getBalance("player-x"));
        assertEquals(1, service2.getWallet("player-x").spinsPlayed());
        assertEquals(500, service2.getWallet("player-x").totalWon());
    }
}
