package com.calefaction.features.games.megaslots;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.economy.EconomyService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MegaSlotsCommand implements SlashCommand {

    private static final Logger log = LoggerFactory.getLogger(MegaSlotsCommand.class);
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final ExecutorService MEGASLOTS_EXECUTOR = Executors.newCachedThreadPool();
    private static final long[] BET_LADDER = {50L, 100L, 250L, 500L, 1000L};

    /** Tracks users currently mid-spin to prevent concurrent plays */
    private final Set<String> activeSpins = ConcurrentHashMap.newKeySet();

    private final CommandRegistry commandRegistry;
    private final EconomyService economyService;

    public MegaSlotsCommand(CommandRegistry commandRegistry, EconomyService economyService) {
        this.commandRegistry = commandRegistry;
        this.economyService = economyService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @PreDestroy
    public void shutdown() {
        MEGASLOTS_EXECUTOR.shutdown();
        try {
            if (!MEGASLOTS_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                MEGASLOTS_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            MEGASLOTS_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("megaslots", "Play the 5-reel Stake-style video slot machine with 243 ways and bonus multipliers!")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL)
                .addOption(OptionType.INTEGER, "bet", "Bet amount in coins (50, 100, 250, 500, 1000). Default: 100", false)
                .addOption(OptionType.BOOLEAN, "bonus_buy", "Instantly buy the 10-spin Bonus Round for 50x bet! Default: false", false);
    }

    @Override
    public String getName() {
        return "megaslots";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();

        if (activeSpins.contains(userId)) {
            event.reply("⏳ Your reels are still spinning! Wait for your current spin to finish.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long bet = 100L;
        OptionMapping betOpt = event.getOption("bet");
        if (betOpt != null) {
            long chosen = betOpt.getAsLong();
            for (long valid : BET_LADDER) {
                if (valid == chosen) {
                    bet = chosen;
                    break;
                }
            }
        }

        boolean bonusBuy = false;
        OptionMapping buyOpt = event.getOption("bonus_buy");
        if (buyOpt != null) {
            bonusBuy = buyOpt.getAsBoolean();
        }

        long requiredCoins = bonusBuy ? bet * 50L : bet;
        long balance = economyService.getBalance(userId);

        if (balance < requiredCoins) {
            event.reply("❌ You need **" + NUMBER_FORMAT.format(requiredCoins) + " 🪙** to play, but your balance is **"
                            + NUMBER_FORMAT.format(balance) + " 🪙**!\nClaim `/daily` or run `/bailout` to get 1,000 coins back.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply().queue();
        playMegaSlots(event.getHook(), event.getUser(), bet, bonusBuy);
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        // Format: megaslots:action:userId:bet
        String[] parts = componentId.split(":");
        if (parts.length < 4 || !"megaslots".equals(parts[0])) {
            return;
        }

        String action = parts[1];
        String ownerId = parts[2];
        long currentBet;
        try {
            currentBet = Long.parseLong(parts[3]);
        } catch (NumberFormatException e) {
            currentBet = 100L;
        }

        if (!event.getUser().getId().equals(ownerId)) {
            event.reply("❌ You cannot control someone else's slot machine! Run `/megaslots` to start your own.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        switch (action) {
            case "spin" -> {
                if (activeSpins.contains(ownerId)) {
                    event.reply("⏳ Your reels are still spinning!").setEphemeral(true).queue();
                    return;
                }
                long balance = economyService.getBalance(ownerId);
                if (balance < currentBet) {
                    event.reply("❌ Not enough coins! You have **" + NUMBER_FORMAT.format(balance)
                                    + " 🪙**, but spin costs **" + NUMBER_FORMAT.format(currentBet) + " 🪙**.\nClaim `/daily` or run `/bailout` to get 1,000 coins back.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                event.deferEdit().queue();
                playMegaSlots(event.getHook(), event.getUser(), currentBet, false);
            }
            case "bet" -> {
                event.deferEdit().queue();
                updateBetDisplay(event.getHook(), event.getUser(), currentBet);
            }
            case "buy" -> {
                if (activeSpins.contains(ownerId)) {
                    event.reply("⏳ Your reels are still spinning!").setEphemeral(true).queue();
                    return;
                }
                long bonusCost = currentBet * 50L;
                long balance = economyService.getBalance(ownerId);
                if (balance < bonusCost) {
                    event.reply("❌ Bonus Buy requires **" + NUMBER_FORMAT.format(bonusCost)
                                    + " 🪙**, but your balance is **" + NUMBER_FORMAT.format(balance) + " 🪙**!")
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                event.deferEdit().queue();
                playMegaSlots(event.getHook(), event.getUser(), currentBet, true);
            }
            case "wallet" -> {
                long balance = economyService.getBalance(ownerId);
                event.reply("💰 **Your Wallet:** **" + NUMBER_FORMAT.format(balance) + " coins**\nDaily reward and bailout: `/coins`")
                        .setEphemeral(true)
                        .queue();
            }
            default -> event.reply("Unknown button action.").setEphemeral(true).queue();
        }
    }

    private void updateBetDisplay(InteractionHook hook, User user, long newBet) {
        long balance = economyService.getBalance(user.getId());
        var embed = MegaSlotsRenderer.buildReadyEmbed(newBet, balance, user);
        hook.editOriginalEmbeds(embed)
                .setComponents(MegaSlotsRenderer.buildActionRows(user.getId(), newBet, false))
                .queue();
    }

    public void playMegaSlots(InteractionHook hook, User user, long bet, boolean bonusBuy) {
        CompletableFuture.runAsync(() -> runMegaSlotsLoop(hook, user, bet, bonusBuy), MEGASLOTS_EXECUTOR);
    }

    private void runMegaSlotsLoop(InteractionHook hook, User user, long bet, boolean bonusBuy) {
        String userId = user.getId();

        // Acquire spin lock
        if (!activeSpins.add(userId)) {
            hook.sendMessage("⏳ Your reels are still spinning!").setEphemeral(true).queue();
            return;
        }

        long cost = bonusBuy ? bet * 50L : bet;

        if (!economyService.deductCoins(userId, cost)) {
            activeSpins.remove(userId);
            hook.sendMessage("❌ Failed to place bet due to insufficient balance.").setEphemeral(true).queue();
            return;
        }

        try {
            var rng = ThreadLocalRandom.current();
            long currentBalance = economyService.getBalance(userId);

            // Phase 1: All 5 reels in motion!
            Message msg = hook.editOriginalEmbeds(
                    MegaSlotsRenderer.buildSpinningEmbed(bet, currentBalance, user))
                    .setComponents() // Clear buttons during spin
                    .complete();

            TimeUnit.MILLISECONDS.sleep(1100);

            // Generate outcome
            VideoSlotGrid initialGrid = VideoSlotEngine.generateGrid(rng, bonusBuy);
            var initialEval = VideoSlotEngine.evaluateSpin(initialGrid, bet);

            // Per-reel stops: Reel 1 → 2 → 3 → 4 → 5
            // Scatter tease ONLY kicks in after 2 scatters are visible on the board
            int cumulativeScatters = 0;
            for (int reelStop = 1; reelStop <= VideoSlotGrid.REELS; reelStop++) {
                // Count scatters on this newly stopped reel
                for (int row = 0; row < VideoSlotGrid.ROWS; row++) {
                    if (initialGrid.getSymbol(reelStop - 1, row) == VideoSlotSymbol.SCATTER) {
                        cumulativeScatters++;
                    }
                }

                // Tease only activates once 2+ scatters are already on the board
                boolean showTease = cumulativeScatters >= 2;

                msg.editMessageEmbeds(
                        MegaSlotsRenderer.buildTeaseEmbed(
                                initialGrid, reelStop, showTease, cumulativeScatters, bet, currentBalance, user))
                        .complete();

                if (reelStop == VideoSlotGrid.REELS) {
                    break; // Last reel — no sleep, proceed to result
                }

                // Timing: flat rhythmic cadence unless scatter tease is active
                long sleepMs;
                if (cumulativeScatters >= 2) {
                    sleepMs = 2000; // Dramatic suspense — hunting for the 3rd scatter
                } else {
                    sleepMs = 400; // Quick rhythmic tick between each reel drop
                }
                TimeUnit.MILLISECONDS.sleep(sleepMs);
            }

            // Phase 4: Multiplier Bomb Detonation (if multiplier landed on win)
            if (initialEval.multiplier() > 1 && initialEval.baseWin() > 0) {
                var baseOnlyEval = new VideoSlotEngine.SpinEvaluation(
                        initialGrid,
                        initialEval.winningWays(),
                        initialEval.baseWin(),
                        1,
                        initialEval.baseWin(),
                        initialEval.scatterCount(),
                        initialEval.triggeredBonus()
                );
                msg.editMessageEmbeds(
                        MegaSlotsRenderer.buildSpinEmbed(baseOnlyEval, bet, currentBalance, user, false, 0, 0, 1))
                        .complete();

                TimeUnit.MILLISECONDS.sleep(1100);

                // Multiplier explosion!
                msg.editMessageEmbeds(
                        MegaSlotsRenderer.buildDetonationEmbed(initialEval, bet, currentBalance, user, initialEval.multiplier()))
                        .complete();

                TimeUnit.MILLISECONDS.sleep(1100);
            }

            // Reveal full grid & base/boosted result
            msg.editMessageEmbeds(
                    MegaSlotsRenderer.buildSpinEmbed(initialEval, bet, currentBalance + initialEval.totalWin(), user, false, 0, 0, 1))
                    .setComponents(MegaSlotsRenderer.buildActionRows(userId, bet, initialEval.triggeredBonus() || bonusBuy))
                    .complete();

            long totalRoundWin = initialEval.totalWin();

            // Phase 5: Super Free Spins Bonus Round (if triggered or bonus buy)
            if (initialEval.triggeredBonus() || bonusBuy) {
                TimeUnit.SECONDS.sleep(1);

                int totalFreeSpins = 10;
                int persistentMultiplier = 1;
                int winningSpins = 0;
                long biggestSpin = 0;
                int peakMultiplier = 1;

                // Show bonus intro transition (gives player time to read feature rules!)
                msg.editMessageEmbeds(
                        MegaSlotsRenderer.buildBonusIntroEmbed(bet, currentBalance + totalRoundWin, user, totalFreeSpins))
                        .complete();

                TimeUnit.MILLISECONDS.sleep(2200);

                for (int spin = 1; spin <= totalFreeSpins; spin++) {
                    TimeUnit.MILLISECONDS.sleep(1500); // 1.5s per free spin

                    VideoSlotGrid freeSpinGrid = VideoSlotEngine.generateGrid(rng, false);
                    var freeSpinEval = VideoSlotEngine.evaluateSpin(freeSpinGrid, bet);

                    // Multiplier bomb accumulation (sticky — carries across spins)
                    if (freeSpinEval.multiplier() > 1) {
                        persistentMultiplier += freeSpinEval.multiplier();
                        peakMultiplier = Math.max(peakMultiplier, persistentMultiplier);
                    }

                    long spinWin = freeSpinEval.baseWin() * (long) persistentMultiplier;
                    totalRoundWin += spinWin;

                    if (spinWin > 0) {
                        winningSpins++;
                        biggestSpin = Math.max(biggestSpin, spinWin);
                    }

                    var finalFreeSpinEval = new VideoSlotEngine.SpinEvaluation(
                            freeSpinGrid,
                            freeSpinEval.winningWays(),
                            freeSpinEval.baseWin(),
                            persistentMultiplier,
                            spinWin,
                            freeSpinEval.scatterCount(),
                            false
                    );

                    msg.editMessageEmbeds(
                            MegaSlotsRenderer.buildSpinEmbed(finalFreeSpinEval, bet, currentBalance + totalRoundWin, user, true, spin, totalFreeSpins, persistentMultiplier))
                            .complete();

                    // Show detonation animation during free spins for large bombs (10x+)
                    if (freeSpinEval.multiplier() >= 10 && freeSpinEval.baseWin() > 0) {
                        TimeUnit.MILLISECONDS.sleep(600);
                        msg.editMessageEmbeds(
                                MegaSlotsRenderer.buildDetonationEmbed(finalFreeSpinEval, bet, currentBalance + totalRoundWin, user, persistentMultiplier))
                                .complete();
                        TimeUnit.MILLISECONDS.sleep(1000);
                    }
                }

                // Show bonus summary
                TimeUnit.MILLISECONDS.sleep(1000);
                msg.editMessageEmbeds(
                        MegaSlotsRenderer.buildBonusSummaryEmbed(bet, totalRoundWin, currentBalance + totalRoundWin,
                                user, totalFreeSpins, winningSpins, biggestSpin, peakMultiplier))
                        .complete();

                TimeUnit.MILLISECONDS.sleep(2500);
            }

            // Record winnings
            economyService.addCoins(userId, totalRoundWin);
            economyService.recordGameResult(userId, cost, totalRoundWin);

            long finalBalance = economyService.getBalance(userId);

            // Restore action buttons with updated bankroll!
            var finalDisplayEval = (initialEval.triggeredBonus() || bonusBuy)
                    ? new VideoSlotEngine.SpinEvaluation(initialGrid, initialEval.winningWays(), initialEval.baseWin(), initialEval.multiplier(), totalRoundWin, initialEval.scatterCount(), false)
                    : initialEval;

            msg.editMessageEmbeds(MegaSlotsRenderer.buildSpinEmbed(finalDisplayEval, bet, finalBalance, user, false, 0, 0, 1))
                    .setComponents(MegaSlotsRenderer.buildActionRows(userId, bet, false))
                    .queue();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Megaslots loop interrupted", e);
        } catch (Exception e) {
            log.error("Error in megaslots loop", e);
            hook.sendMessage("An error occurred during the slot spin!").setEphemeral(true).queue();
        } finally {
            activeSpins.remove(userId);
        }
    }
}
