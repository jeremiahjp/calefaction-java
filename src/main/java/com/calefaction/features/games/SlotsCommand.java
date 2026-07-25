package com.calefaction.features.games;

import com.calefaction.config.BotProperties;
import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SlotsCommand implements SlashCommand {

    private static final Logger log = LoggerFactory.getLogger(SlotsCommand.class);
    private final CommandRegistry commandRegistry;
    private final BotProperties botProperties;
    private final Random random = new Random();
    private static final ExecutorService SLOTS_EXECUTOR = Executors.newCachedThreadPool();
    private static final List<String> EMOJIS = List.of(
            "🆓", "🍒", "🍋", "🍇", "🔔", "💎", "7️⃣",
            "🍊", "🍉", "🍎", "🍌", "🍓", "🍍",
            "⭐", "🍀", "👑", "💰", "🎲", "🔥");
    private static final String SPINNING = "🌀";

    public SlotsCommand(CommandRegistry commandRegistry, BotProperties botProperties) {
        this.commandRegistry = commandRegistry;
        this.botProperties = botProperties;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("slots", "Play the deluxe slot machine!")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM,
                        InteractionContextType.PRIVATE_CHANNEL);
    }

    @Override
    public String getName() {
        return "slots";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        playSlots(event.getHook(), event.getUser());
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        // Format: slots:spin:userId

        if (parts.length == 3 && parts[0].equals("slots") && parts[1].equals("spin")) {
            String ownerId = parts[2];

            if (!event.getUser().getId().equals(ownerId)) {
                event.reply("❌ You cannot spin someone else's slot machine! Run `/slots` to start your own.")
                        .setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();
            playSlots(event.getHook(), event.getUser());
        }
    }

    private void playSlots(InteractionHook hook, User user) {
        CompletableFuture.runAsync(() -> runGameLoop(hook, user), SLOTS_EXECUTOR);
    }

    private void runGameLoop(InteractionHook hook, User user) {
        try {
            // Initial Setup
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎰 Super Slots 🎰")
                    .setDescription(renderMachine(SPINNING, SPINNING, SPINNING))
                    .setColor(Color.DARK_GRAY)
                    .setFooter("Initializing machine...");

            // Send or Edit to get the Message object
            // We use editOriginalEmbeds because execute/onButton deferred.
            Message msg = hook.editOriginalEmbeds(embed.build())
                    .setComponents() // Clear buttons
                    .complete();

            // Run First Spin
            SpinResult result = executeSpin(msg, user, 0, 0);

            // Check for Free Spins
            if (result.triggeredFreeSpins) {
                int totalFreeSpins = 10;
                int currentFreeSpin = 0;

                while (currentFreeSpin < totalFreeSpins) {
                    currentFreeSpin++;
                    TimeUnit.SECONDS.sleep(1); // 1s Delay between spins

                    // Execute Free Spin
                    SpinResult fsResult = executeSpin(msg, user, currentFreeSpin, totalFreeSpins);

                    // If re-triggered, extend?
                    if (fsResult.triggeredFreeSpins) {
                        totalFreeSpins += 10;
                        // Optional notification?
                    }
                }
            }

            // Game Over (Chain finished) -> Restore Button
            msg.editMessageComponents(ActionRow.of(
                    Button.primary("slots:spin:" + user.getId(), "SPIN"))).queue();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Slots loop interrupted", e);
        } catch (Exception e) {
            log.error("Error in slots game loop", e);
            hook.sendMessage("Something went wrong with the slot machine!").setEphemeral(true).queue();
        }
    }

    private SpinResult executeSpin(Message msg, User user, int currentSpin, int totalSpins)
            throws InterruptedException {
        // RTP Logic
        double outcome = random.nextDouble();
        String s1, s2, s3;
        boolean triggeredFreeSpins = false;

        boolean isDev = botProperties.isAdmin(user.getId());
        boolean forceFree = isDev && random.nextDouble() < 0.5;

        if (forceFree || outcome < 0.0076) {
            if (forceFree) {
                s1 = "🆓";
                s2 = "🆓";
                s3 = "🆓";
                triggeredFreeSpins = true;
            } else {
                String symbol = EMOJIS.get(random.nextInt(EMOJIS.size()));
                if (symbol.equals("🆓"))
                    symbol = "💎";
                s1 = symbol;
                s2 = symbol;
                s3 = symbol;
            }
        } else if (outcome < 0.0276) {
            s1 = "🆓";
            s2 = "🆓";
            s3 = "🆓";
            triggeredFreeSpins = true;
        } else if (outcome < 0.2076) {
            String match = EMOJIS.get(random.nextInt(EMOJIS.size()));
            if (match.equals("🆓"))
                match = "🍒";
            String other;
            do {
                other = EMOJIS.get(random.nextInt(EMOJIS.size()));
            } while (other.equals(match) || other.equals("🆓"));

            int pos = random.nextInt(3);
            if (pos == 0) {
                s1 = match;
                s2 = match;
                s3 = other;
            } else if (pos == 1) {
                s1 = match;
                s2 = other;
                s3 = match;
            } else {
                s1 = other;
                s2 = match;
                s3 = match;
            }
        } else {
            do {
                s1 = EMOJIS.get(random.nextInt(EMOJIS.size()));
                s2 = EMOJIS.get(random.nextInt(EMOJIS.size()));
                s3 = EMOJIS.get(random.nextInt(EMOJIS.size()));
            } while ((s1.equals(s2) && s2.equals(s3)) || (s1.equals(s2) || s2.equals(s3) || s1.equals(s3)));
        }

        boolean isFreeSpinTrigger = triggeredFreeSpins; // Alias
        boolean isSuspense = s1.equals(s2);
        boolean isJackpot = s1.equals(s2) && s2.equals(s3) && !isFreeSpinTrigger;
        boolean isSmallWin = !isJackpot && !isFreeSpinTrigger && (s1.equals(s2) || s2.equals(s3) || s1.equals(s3));
        boolean isPlayingFreeSpin = totalSpins > 0;

        // Base Embed
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎰 Super Slots 🎰")
                .setDescription(renderMachine(SPINNING, SPINNING, SPINNING))
                .setColor(Color.DARK_GRAY);

        if (isPlayingFreeSpin) {
            embed.setFooter(String.format("Free Spin %d/%d", currentSpin, totalSpins));
            embed.setColor(Color.CYAN);
        } else {
            embed.setFooter("Spinning...");
        }

        // Animation Phase 1
        for (int i = 0; i < 3; i++) {
            TimeUnit.MILLISECONDS.sleep(300);
            String r1 = EMOJIS.get(random.nextInt(EMOJIS.size()));
            String r2 = EMOJIS.get(random.nextInt(EMOJIS.size()));
            String r3 = EMOJIS.get(random.nextInt(EMOJIS.size()));
            msg.editMessageEmbeds(embed.setDescription(renderMachine(r1, r2, r3)).build()).complete();
        }

        // Reveal 1
        TimeUnit.MILLISECONDS.sleep(400);
        msg.editMessageEmbeds(embed.setDescription(renderMachine(s1, SPINNING, SPINNING)).build()).complete();

        // Reveal 2
        TimeUnit.MILLISECONDS.sleep(400);
        msg.editMessageEmbeds(embed.setDescription(renderMachine(s1, s2, SPINNING)).build()).complete();

        // Suspense
        if (isSuspense) {
            embed.setFooter("👀 SUSPENSE... SO CLOSE!");
            embed.setColor(Color.ORANGE);
            msg.editMessageEmbeds(embed.setDescription(renderMachine(s1, s2, SPINNING)).build()).complete();
            TimeUnit.MILLISECONDS.sleep(2000);
        } else {
            TimeUnit.MILLISECONDS.sleep(400);
        }

        // Final Reveal
        msg.editMessageEmbeds(embed.setDescription(renderMachine(s1, s2, s3)).build()).complete();

        // Result Text & Color
        String result;
        Color finalColor;

        if (isFreeSpinTrigger) {
            result = "🎉 **FREE SPINS TRIGGERED!** 🎉\nStarting " + (isPlayingFreeSpin ? "10 MORE" : "10")
                    + " Automatic Free Spins!";
            finalColor = Color.MAGENTA;
        } else if (isJackpot) {
            result = "🎉 **JACKPOT!** 🎉\nYou hit the big one!";
            finalColor = Color.GREEN;
        } else if (isSmallWin) {
            result = "✨ **Nice!** ✨\nA solid win!";
            finalColor = Color.YELLOW;
        } else {
            result = "❌ **Unlucky** ❌\nTry again!";
            finalColor = Color.RED;
        }

        embed.setDescription(renderMachine(s1, s2, s3))
                .addField("Result", result, false)
                .setColor(finalColor)
                .setFooter(user.getName() + " played the slots", user.getAvatarUrl());

        if (isPlayingFreeSpin) {
            embed.setFooter(String.format("Free Spin %d/%d", currentSpin, totalSpins));
            // If triggered inside, override color
            if (isFreeSpinTrigger)
                embed.setColor(Color.MAGENTA);
        }

        msg.editMessageEmbeds(embed.build()).complete();

        // Flash Effect for Wins
        if (isJackpot || isFreeSpinTrigger) {
            Color flashColor = isFreeSpinTrigger ? Color.MAGENTA : Color.GREEN;
            for (int i = 0; i < 3; i++) {
                TimeUnit.MILLISECONDS.sleep(300);
                embed.setColor(Color.WHITE);
                msg.editMessageEmbeds(embed.build()).complete();
                TimeUnit.MILLISECONDS.sleep(300);
                embed.setColor(flashColor);
                msg.editMessageEmbeds(embed.build()).complete();
            }
        }

        return new SpinResult(isFreeSpinTrigger);
    }

    private String renderMachine(String s1, String s2, String s3) {
        return String.format("# \u00A0\u00A0%s\u00A0\u00A0|\u00A0\u00A0%s\u00A0\u00A0|\u00A0\u00A0%s", s1, s2, s3);
    }

    private record SpinResult(boolean triggeredFreeSpins) {
    }
}
