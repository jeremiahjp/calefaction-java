package com.calefaction.features.games;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.economy.EconomyService;
import com.calefaction.features.games.contexto.ContextoCommand;
import com.calefaction.features.games.megaslots.MegaSlotsCommand;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class GamesCommand implements SlashCommand {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private final CommandRegistry commandRegistry;
    private final ContextoCommand contextoCommand;
    private final SlotsCommand slotsCommand;
    private final MegaSlotsCommand megaSlotsCommand;
    private final EconomyService economyService;

    public GamesCommand(
            CommandRegistry commandRegistry,
            ContextoCommand contextoCommand,
            SlotsCommand slotsCommand,
            MegaSlotsCommand megaSlotsCommand,
            EconomyService economyService
    ) {
        this.commandRegistry = commandRegistry;
        this.contextoCommand = contextoCommand;
        this.slotsCommand = slotsCommand;
        this.megaSlotsCommand = megaSlotsCommand;
        this.economyService = economyService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("games", "Browse all available bot games and jump right into one!")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL)
                .addOption(OptionType.STRING, "game", "Directly launch a specific game", false, true);
    }

    @Override
    public String getName() {
        return "games";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping gameOpt = event.getOption("game");
        if (gameOpt != null) {
            String selectedGame = gameOpt.getAsString().toLowerCase().trim();
            launchGame(selectedGame, event);
            return;
        }

        boolean contextoDisabled = commandRegistry.isCommandDisabled("contexto");
        boolean slotsDisabled = commandRegistry.isCommandDisabled("slots");
        boolean megaSlotsDisabled = commandRegistry.isCommandDisabled("megaslots");

        long userBalance = economyService.getBalance(event.getUser().getId());

        MessageEmbed embed = buildGamesHubEmbed(contextoDisabled, slotsDisabled, megaSlotsDisabled, userBalance);
        ActionRow buttons = buildActionRow(contextoDisabled, slotsDisabled, megaSlotsDisabled);

        event.replyEmbeds(embed)
                .setComponents(buttons)
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if ("game".equals(event.getFocusedOption().getName())) {
            String input = event.getFocusedOption().getValue().toLowerCase().trim();

            List<Command.Choice> allChoices = List.of(
                    new Command.Choice("🧩 Contexto — Daily Word Puzzle", "contexto"),
                    new Command.Choice("🎰 Super Slots — Classic 3-Reel Machine", "slots"),
                    new Command.Choice("⚡ Super Megaslots — 5-Reel 243-Ways Stake Video Slot", "megaslots")
            );

            List<Command.Choice> filtered = allChoices.stream()
                    .filter(c -> c.getName().toLowerCase().contains(input) || c.getAsString().toLowerCase().contains(input))
                    .toList();

            event.replyChoices(filtered).queue();
        }
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        // Format: games:play:gameId
        String[] parts = componentId.split(":");
        if (parts.length < 3 || !"games".equals(parts[0]) || !"play".equals(parts[1])) {
            return;
        }

        String gameId = parts[2];
        switch (gameId) {
            case "contexto" -> {
                if (commandRegistry.isCommandDisabled("contexto")) {
                    event.reply("❌ Contexto is currently disabled.").setEphemeral(true).queue();
                    return;
                }
                contextoCommand.handlePlay(event);
            }
            case "slots" -> {
                if (commandRegistry.isCommandDisabled("slots")) {
                    event.reply("❌ Classic Slots is currently disabled.").setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue(hook -> slotsCommand.playSlots(hook, event.getUser()));
            }
            case "megaslots" -> {
                if (commandRegistry.isCommandDisabled("megaslots")) {
                    event.reply("❌ Super Megaslots is currently disabled.").setEphemeral(true).queue();
                    return;
                }
                long balance = economyService.getBalance(event.getUser().getId());
                if (balance < 100L) {
                    event.reply("❌ You have **" + NUMBER_FORMAT.format(balance)
                                    + " 🪙**, but spin costs **100 🪙**!\nClaim your free daily allowance with `/daily` or reload 1,000 coins with `/bailout`.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                event.deferReply().queue(hook -> megaSlotsCommand.playMegaSlots(hook, event.getUser(), 100L, false));
            }
            default -> event.reply("❌ Unknown game.").setEphemeral(true).queue();
        }
    }

    private void launchGame(String gameId, SlashCommandInteractionEvent event) {
        switch (gameId) {
            case "contexto" -> {
                if (commandRegistry.isCommandDisabled("contexto")) {
                    event.reply("❌ Contexto is currently disabled.").setEphemeral(true).queue();
                    return;
                }
                contextoCommand.handlePlay(event);
            }
            case "slots" -> {
                if (commandRegistry.isCommandDisabled("slots")) {
                    event.reply("❌ Classic Slots is currently disabled.").setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();
                slotsCommand.playSlots(event.getHook(), event.getUser());
            }
            case "megaslots" -> {
                if (commandRegistry.isCommandDisabled("megaslots")) {
                    event.reply("❌ Super Megaslots is currently disabled.").setEphemeral(true).queue();
                    return;
                }
                long balance = economyService.getBalance(event.getUser().getId());
                if (balance < 100L) {
                    event.reply("❌ You need **100 🪙** to spin, but have **" + NUMBER_FORMAT.format(balance) + " 🪙**!\nClaim `/coins daily` to reload.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                event.deferReply().queue();
                megaSlotsCommand.playMegaSlots(event.getHook(), event.getUser(), 100L, false);
            }
            default -> event.reply("❌ Unknown game: `" + gameId + "`. Use `/games` to see all available games.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    public static MessageEmbed buildGamesHubEmbed(
            boolean contextoDisabled,
            boolean slotsDisabled,
            boolean megaSlotsDisabled,
            long userBalance
    ) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🎮 Calefaction Games Arcade");
        eb.setColor(new Color(114, 137, 218)); // Discord Blurple
        eb.setDescription("Welcome to the bot arcade! Jump right into any game using the buttons below.\n"
                + "💰 **Your Bankroll:** **" + NUMBER_FORMAT.format(userBalance) + " 🪙** *(Claim daily coins with `/coins daily`)*\n");

        // Contexto
        String contextoStatus = contextoDisabled ? "🔴 **Disabled**" : "🟢 **Available**";
        eb.addField("🧩 Contexto",
                "• **Status:** " + contextoStatus + " | **Type:** Daily Semantic Word Puzzle\n"
                        + "• **Objective:** Guess words to uncover the secret word based on context similarity.\n"
                        + "• **Commands:** `/contexto play` • `/contexto leaderboard` • `/contexto share`",
                false);

        // MegaSlots (Stake Edition)
        String megaSlotsStatus = megaSlotsDisabled ? "🔴 **Disabled**" : "🟢 **Available**";
        eb.addField("⚡ Super Megaslots (Stake Edition)",
                "• **Status:** " + megaSlotsStatus + " | **Type:** 5-Reel 243-Ways Video Slot\n"
                        + "• **Features:** Multiplier Bombs (`2x`–`100x`), Wilds, 10 Super Free Spins with Sticky Multipliers, and instant Bonus Buy!\n"
                        + "• **Commands:** `/megaslots [bet] [bonus_buy]`",
                false);

        // Classic Slots
        String slotsStatus = slotsDisabled ? "🔴 **Disabled**" : "🟢 **Available**";
        eb.addField("🎰 Classic Super Slots",
                "• **Status:** " + slotsStatus + " | **Type:** 3-Reel Arcade Slot\n"
                        + "• **Features:** Realistic suspense animations, jackpots, and free spin chains (`🆓 🆓 🆓`).\n"
                        + "• **Commands:** `/slots`",
                false);

        eb.setFooter("Calefaction Arcade • Choose a game to play!");
        return eb.build();
    }

    public static ActionRow buildActionRow(
            boolean contextoDisabled,
            boolean slotsDisabled,
            boolean megaSlotsDisabled
    ) {
        Button contextoBtn = Button.primary("games:play:contexto", "🧩 Play Contexto");
        if (contextoDisabled) contextoBtn = contextoBtn.asDisabled();

        Button megaSlotsBtn = Button.danger("games:play:megaslots", "⚡ Super Megaslots");
        if (megaSlotsDisabled) megaSlotsBtn = megaSlotsBtn.asDisabled();

        Button slotsBtn = Button.success("games:play:slots", "🎰 Classic Slots");
        if (slotsDisabled) slotsBtn = slotsBtn.asDisabled();

        return ActionRow.of(contextoBtn, megaSlotsBtn, slotsBtn);
    }
}
