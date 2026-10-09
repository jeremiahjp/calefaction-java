package com.calefaction.features.economy;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.economy.model.UserWallet;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class EconomyCommand implements SlashCommand {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private final CommandRegistry commandRegistry;
    private final EconomyService economyService;

    public EconomyCommand(CommandRegistry commandRegistry, EconomyService economyService) {
        this.commandRegistry = commandRegistry;
        this.economyService = economyService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("coins", "Manage your bot coins, daily allowance, and casino bankroll")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL)
                .addSubcommands(
                        new SubcommandData("balance", "Check your coin balance, stats, and streaks"),
                        new SubcommandData("daily", "Claim your free daily coin allowance"),
                        new SubcommandData("bailout", "Claim 1,000 emergency coins if your balance is under 100"),
                        new SubcommandData("leaderboard", "View the wealthiest players on the bot")
                );
    }

    @Override
    public String getName() {
        return "coins";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) {
            sub = "balance";
        }
        String userId = event.getUser().getId();

        switch (sub) {
            case "balance" -> handleBalance(event, userId);
            case "daily" -> handleDaily(event, userId);
            case "bailout" -> handleBailout(event, userId);
            case "leaderboard" -> handleLeaderboard(event);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        // Format: economy:action:userId
        String[] parts = componentId.split(":");
        if (parts.length < 3 || !"economy".equals(parts[0])) {
            return;
        }

        String action = parts[1];
        String targetUserId = parts[2];

        if (!event.getUser().getId().equals(targetUserId)) {
            event.reply("❌ This menu is for another player! Run `/coins` to open your own.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        switch (action) {
            case "daily" -> {
                var result = economyService.claimDaily(targetUserId);
                if (result.success()) {
                    event.reply("🎁 **Daily Claimed!** You received **" + NUMBER_FORMAT.format(result.amountClaimed()) + " coins**! (Streak: " + result.streak() + " days)")
                            .setEphemeral(true)
                            .queue();
                } else {
                    long hours = result.timeUntilNext().toHours();
                    long minutes = result.timeUntilNext().toMinutesPart();
                    event.reply("⏳ " + result.message() + " Ready in **" + hours + "h " + minutes + "m**.")
                            .setEphemeral(true)
                            .queue();
                }
            }
            case "bailout" -> {
                var result = economyService.claimBailout(targetUserId);
                event.reply(result.message()).setEphemeral(true).queue();
            }
            case "refresh" -> {
                UserWallet wallet = economyService.getWallet(targetUserId);
                event.editMessageEmbeds(buildBalanceEmbed(wallet, event.getUser()))
                        .setComponents(buildActionRow(targetUserId, wallet.balance()))
                        .queue();
            }
            default -> event.reply("Unknown button action.").setEphemeral(true).queue();
        }
    }

    private void handleBalance(SlashCommandInteractionEvent event, String userId) {
        UserWallet wallet = economyService.getWallet(userId);
        event.replyEmbeds(buildBalanceEmbed(wallet, event.getUser()))
                .setComponents(buildActionRow(userId, wallet.balance()))
                .setEphemeral(true)
                .queue();
    }

    private void handleDaily(SlashCommandInteractionEvent event, String userId) {
        var result = economyService.claimDaily(userId);
        if (result.success()) {
            UserWallet wallet = economyService.getWallet(userId);
            event.replyEmbeds(buildBalanceEmbed(wallet, event.getUser()))
                    .setComponents(buildActionRow(userId, wallet.balance()))
                    .queue();
        } else {
            long hours = result.timeUntilNext().toHours();
            long minutes = result.timeUntilNext().toMinutesPart();
            event.reply("⏳ " + result.message() + " Next daily reward available in **" + hours + "h " + minutes + "m**.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void handleBailout(SlashCommandInteractionEvent event, String userId) {
        var result = economyService.claimBailout(userId);
        event.reply(result.message()).setEphemeral(true).queue();
    }

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        List<UserWallet> top = economyService.getTopWallets(10);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🏆 Calefaction Coin Leaderboard");
        eb.setColor(new Color(241, 196, 15)); // Gold

        if (top.isEmpty()) {
            eb.setDescription("No wallets recorded yet! Play some games to join the board.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < top.size(); i++) {
                UserWallet w = top.get(i);
                String medal;
                if (i == 0) medal = "🥇";
                else if (i == 1) medal = "🥈";
                else if (i == 2) medal = "🥉";
                else medal = String.format("`%2d.`", i + 1);

                sb.append(String.format("%s <@%s> — **%s** 🪙 *(%d spins)*\n",
                        medal, w.userId(), NUMBER_FORMAT.format(w.balance()), w.spinsPlayed()));
            }
            eb.setDescription(sb.toString());
        }
        eb.setFooter("Calefaction Economy • Use /coins to view your wallet");
        event.replyEmbeds(eb.build()).queue();
    }

    public static MessageEmbed buildBalanceEmbed(UserWallet wallet, User user) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🪙 " + user.getEffectiveName() + "'s Wallet");
        eb.setColor(new Color(255, 215, 0)); // Gold

        eb.setDescription("### Balance: **" + NUMBER_FORMAT.format(wallet.balance()) + " 🪙**\n"
                + "Use coins to play high-roller slots, enter bonus rounds, and climb the leaderboard!");

        eb.addField("Daily Streak", "🔥 **" + wallet.dailyStreak() + " days**", true);
        eb.addField("Spins Played", "🎰 **" + NUMBER_FORMAT.format(wallet.spinsPlayed()) + "**", true);
        eb.addField("Total Won", "📈 **" + NUMBER_FORMAT.format(wallet.totalWon()) + " 🪙**", true);

        if (wallet.totalBet() > 0) {
            double profitLoss = (double) wallet.totalWon() - wallet.totalBet();
            String pnlSign = profitLoss >= 0 ? "+" : "";
            eb.addField("Net P&L", String.format("💰 **%s%s 🪙**", pnlSign, NUMBER_FORMAT.format((long) profitLoss)), true);
        }

        eb.setFooter("Calefaction Economy System", user.getEffectiveAvatarUrl());
        return eb.build();
    }

    public static ActionRow buildActionRow(String userId, long currentBalance) {
        Button dailyBtn = Button.primary("economy:daily:" + userId, "🎁 Claim Daily");
        Button refreshBtn = Button.secondary("economy:refresh:" + userId, "🔄 Refresh");

        if (currentBalance < EconomyService.BAILOUT_THRESHOLD) {
            Button bailoutBtn = Button.danger("economy:bailout:" + userId, "🚨 Emergency Bailout (1,000 🪙)");
            return ActionRow.of(dailyBtn, bailoutBtn, refreshBtn);
        }

        return ActionRow.of(dailyBtn, refreshBtn);
    }
}
