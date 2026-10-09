package com.calefaction.features.games.megaslots;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class MegaSlotsRenderer {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    public static MessageEmbed buildSpinningEmbed(long betAmount, long userBalance, User user) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🎰 Spinning the Reels... 🎰");
        eb.setColor(new Color(50, 50, 60));

        String spinningGrid = "╔═══════════════════════════╗\n"
                + "║ 🌀 │ 🌀 │ 🌀 │ 🌀 │ 🌀 ║\n"
                + "║ 🌀 │ 🌀 │ 🌀 │ 🌀 │ 🌀 ║\n"
                + "║ 🌀 │ 🌀 │ 🌀 │ 🌀 │ 🌀 ║\n"
                + "╚═══════════════════════════╝";

        eb.setDescription("```\n" + spinningGrid + "\n```\n⚡ *Reels in motion across 243 ways...*");
        eb.addField("Bet", "🪙 **" + NUMBER_FORMAT.format(betAmount) + "**", true);
        eb.addField("Bankroll", "💰 **" + NUMBER_FORMAT.format(userBalance) + " 🪙**", true);
        eb.setFooter(user != null ? user.getEffectiveName() + " is spinning" : "Calefaction Megaslots");

        return eb.build();
    }

    public static MessageEmbed buildTeaseEmbed(
            VideoSlotGrid grid,
            int stoppedReels,
            boolean isScatterTease,
            int scatterCountSoFar,
            long betAmount,
            long userBalance,
            User user
    ) {
        EmbedBuilder eb = new EmbedBuilder();

        if (isScatterTease) {
            if (scatterCountSoFar >= 3) {
                eb.setTitle("🌟 ⭐ 3RD SCATTER LOCKED! BONUS UNLOCKED! ⭐ 🌟");
            } else {
                eb.setTitle("👀 SCATTER TEASE! LOOKING FOR BONUS ⭐");
            }
            eb.setColor(new Color(255, 140, 0)); // Bright Orange
        } else {
            switch (stoppedReels) {
                case 1 -> { eb.setTitle("🎰 REEL 1 LOCKED IN! 🎰"); eb.setColor(new Color(52, 152, 219)); }
                case 2 -> { eb.setTitle("🔄 REELS 1–2 LOCKED! 🔄"); eb.setColor(new Color(52, 152, 219)); }
                case 3 -> { eb.setTitle("🔥 REELS 1–3 LOCKED! 🔥"); eb.setColor(new Color(241, 196, 15)); }
                case 4 -> { eb.setTitle("⚡ REEL 4 LOCKED IN! ⚡"); eb.setColor(new Color(241, 196, 15)); }
                default -> { eb.setTitle("💥 ALL REELS LOCKED! 💥"); eb.setColor(new Color(241, 196, 15)); }
            }
        }

        StringBuilder desc = new StringBuilder();
        desc.append("```\n").append(grid.renderWithSpinningReels(stoppedReels)).append("\n```\n");

        if (isScatterTease) {
            if (scatterCountSoFar >= 3) {
                desc.append("🌟 **3 SCATTERS ALREADY LOCKED!** 🌟\n**10 Super Free Spins** are secured! Spinning remaining reels for extra scatters...\n");
            } else if (stoppedReels >= 4) {
                desc.append("🔥 **2 SCATTERS LOCKED ACROSS REELS 1–").append(stoppedReels).append("!** 🔥\nFinal reel decelerating... Will the **3rd Scatter** drop?!\n");
            } else {
                desc.append("⭐ **").append(scatterCountSoFar).append(" SCATTER").append(scatterCountSoFar != 1 ? "S" : "").append(" LOCKED SO FAR!** ⭐\nRemaining reels spinning for the **10 Super Free Spins Bonus**... Hold your breath!\n");
            }
        } else {
            switch (stoppedReels) {
                case 1 -> desc.append("✨ Reel 1 stopped! 4 reels still spinning across 243 ways...\n");
                case 2 -> desc.append("✨ Reels 1–2 locked in! 3 reels still connecting combinations...\n");
                case 3 -> desc.append("✨ Reels 1–3 locked in! Connecting potential winning ways across reels 4 & 5...\n");
                case 4 -> desc.append("✨ Reels 1–4 locked! Reel 5 decelerating to complete 243-way combinations...\n");
                default -> desc.append("✨ All reels locked! Evaluating wins...\n");
            }
        }

        eb.setDescription(desc.toString());
        eb.addField("Bet", "🪙 **" + NUMBER_FORMAT.format(betAmount) + "**", true);
        eb.addField("Bankroll", "💰 **" + NUMBER_FORMAT.format(userBalance) + " 🪙**", true);

        String footer;
        if (stoppedReels >= 4) {
            footer = "FINAL REEL • Reel 5 coming to a stop...";
        } else if (stoppedReels == 3) {
            footer = "SUSPENSE ROLL • Reels 4 & 5 decelerating...";
        } else {
            footer = "REEL " + stoppedReels + " LOCKED • " + (VideoSlotGrid.REELS - stoppedReels) + " reels still spinning...";
        }
        eb.setFooter(footer);

        return eb.build();
    }

    public static MessageEmbed buildTeaseEmbed(
            VideoSlotGrid grid,
            int stoppedReels,
            boolean isScatterTease,
            long betAmount,
            long userBalance,
            User user
    ) {
        return buildTeaseEmbed(grid, stoppedReels, isScatterTease, isScatterTease ? 2 : 0, betAmount, userBalance, user);
    }

    public static MessageEmbed buildDetonationEmbed(
            VideoSlotEngine.SpinEvaluation evaluation,
            long betAmount,
            long userBalance,
            User user,
            int multiplier
    ) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("💥 MULTIPLIER DETONATION! 💥");
        eb.setColor(new Color(233, 30, 99)); // Neon Magenta / Electric Pink

        StringBuilder desc = new StringBuilder();
        desc.append("```\n").append(evaluation.grid().render()).append("\n```\n");
        desc.append("💣 **MULTIPLIER BOMB DETONATED!**\n");
        desc.append(String.format("Base Win: **%s 🪙** ➔ Multiplied by **x%d** ➔ 🎉 **%s 🪙!**\n",
                NUMBER_FORMAT.format(evaluation.baseWin()), multiplier, NUMBER_FORMAT.format(evaluation.totalWin())));

        eb.setDescription(desc.toString());
        eb.addField("Bet", "🪙 **" + NUMBER_FORMAT.format(betAmount) + "**", true);
        eb.addField("Bankroll", "💰 **" + NUMBER_FORMAT.format(userBalance) + " 🪙**", true);
        eb.setFooter("Stake Multiplier Drop • Boom!");

        return eb.build();
    }

    public static MessageEmbed buildSpinEmbed(
            VideoSlotEngine.SpinEvaluation evaluation,
            long betAmount,
            long userBalance,
            User user,
            boolean isFreeSpin,
            int currentFreeSpin,
            int totalFreeSpins,
            int persistentMultiplier
    ) {
        EmbedBuilder eb = new EmbedBuilder();

        // Dopamine title & color tiers
        if (evaluation.triggeredBonus()) {
            eb.setTitle("🌟 ⭐ BONUS ROUND UNLOCKED! ⭐ 🌟");
            eb.setColor(new Color(155, 89, 182)); // Majestic Purple
        } else if (isFreeSpin) {
            eb.setTitle(String.format("⚡ SUPER FREE SPINS (%d/%d) ⚡", currentFreeSpin, totalFreeSpins));
            eb.setColor(new Color(155, 89, 182));
        } else if (evaluation.totalWin() >= betAmount * 50) {
            eb.setTitle("🏆 🔥 MEGA JACKPOT WIN! 🔥 🏆");
            eb.setColor(new Color(255, 215, 0)); // Pure Gold
        } else if (evaluation.totalWin() >= betAmount * 25) {
            eb.setTitle("🚨 💎 SENSATIONAL SUPER WIN! 💎 🚨");
            eb.setColor(new Color(0, 255, 255)); // Cyan / Diamond
        } else if (evaluation.totalWin() >= betAmount * 10) {
            eb.setTitle("✨ 👑 BIG WIN! 👑 ✨");
            eb.setColor(new Color(241, 196, 15)); // Gold
        } else if (evaluation.totalWin() > 0) {
            eb.setTitle("🎉 WINNER! 🎉");
            eb.setColor(new Color(46, 204, 113)); // Emerald Green
        } else {
            eb.setTitle("⚡ Super Megaslots ⚡");
            eb.setColor(new Color(44, 47, 51)); // Dark Slate
        }

        // Render Grid
        StringBuilder desc = new StringBuilder();
        desc.append("```\n").append(evaluation.grid().render()).append("\n```\n");

        if (evaluation.triggeredBonus()) {
            desc.append("🌟 **3+ SCATTERS LANDED!** 🌟\nStarting **10 Super Free Spins** with Sticky Accumulating Multipliers!\n\n");
        }

        if (evaluation.totalWin() > 0) {
            double xBet = betAmount > 0 ? (double) evaluation.totalWin() / betAmount : 0;
            if (evaluation.totalWin() >= betAmount * 50) {
                desc.append("# 🏆 🔥 MEGA JACKPOT PAYOUT! 🔥 🏆\n");
                desc.append("## 💎 💰 **").append(NUMBER_FORMAT.format(evaluation.totalWin())).append(" 🪙** (")
                        .append(String.format(Locale.US, "%.1fx bet", xBet)).append(") 💰 💎\n");
                desc.append("🎊 *UNBELIEVABLE HIT! Massive connection across 243 ways!* 🎊\n\n");
            } else if (evaluation.totalWin() >= betAmount * 25) {
                desc.append("## 🚨 💎 SENSATIONAL SUPER WIN! 💎 🚨\n");
                desc.append("### 💰 **").append(NUMBER_FORMAT.format(evaluation.totalWin())).append(" 🪙** (")
                        .append(String.format(Locale.US, "%.1fx bet", xBet)).append(")\n");
                desc.append("🔥 *Massive symbol connection! Huge payout awarded!* 🔥\n\n");
            } else if (evaluation.totalWin() >= betAmount * 10) {
                desc.append("### ✨ 👑 BIG WIN: **").append(NUMBER_FORMAT.format(evaluation.totalWin())).append(" 🪙** (")
                        .append(String.format(Locale.US, "%.1fx bet", xBet)).append(") ✨\n\n");
            } else {
                desc.append("### 💰 WIN: **").append(NUMBER_FORMAT.format(evaluation.totalWin())).append(" 🪙**\n");
            }

            if (!evaluation.winningWays().isEmpty()) {
                if (evaluation.winningWays().size() <= 4) {
                    for (var way : evaluation.winningWays()) {
                        desc.append(String.format("• %dx %s **%s** — Reels 1➔%d connected (%d ways) ➔ **%s 🪙**\n",
                                way.matchCount(), way.symbol().getEmoji(), way.symbol().getName(),
                                way.matchCount(), way.waysCount(), NUMBER_FORMAT.format(way.payout())));
                    }
                } else {
                    desc.append(String.format("• Connected **%d** winning paths across 243 ways!\n", evaluation.winningWays().size()));
                    for (int i = 0; i < 3 && i < evaluation.winningWays().size(); i++) {
                        var way = evaluation.winningWays().get(i);
                        desc.append(String.format("  └ %dx %s %s (%d ways) ➔ **%s 🪙**\n",
                                way.matchCount(), way.symbol().getEmoji(), way.symbol().getName(),
                                way.waysCount(), NUMBER_FORMAT.format(way.payout())));
                    }
                    if (evaluation.winningWays().size() > 3) {
                        desc.append(String.format("  └ *...and %d more paths!*\n", evaluation.winningWays().size() - 3));
                    }
                }
            }

            if (evaluation.scatterCount() >= 3) {
                long scPayout = (evaluation.scatterCount() == 3 ? 3L : evaluation.scatterCount() == 4 ? 15L : 100L) * betAmount;
                desc.append(String.format("• %dx ⭐ **Scatters** anywhere on reels ➔ **%s 🪙** + 10 Free Spins! 🌟\n",
                        evaluation.scatterCount(), NUMBER_FORMAT.format(scPayout)));
            }

            int activeMult = Math.max(evaluation.multiplier(), persistentMultiplier);
            if (activeMult > 1 && evaluation.baseWin() > 0) {
                desc.append(String.format("\n💥 **Multiplier Bomb Active:** **x%d multiplier** applied to ways win!\n", activeMult));
            }
        } else if (!evaluation.triggeredBonus()) {
            desc.append("❌ No winning paths. Spin again!\n");
        }

        eb.setDescription(desc.toString());

        // Info Fields
        eb.addField("Bet", "🪙 **" + NUMBER_FORMAT.format(betAmount) + "**", true);
        eb.addField("Bankroll", "💰 **" + NUMBER_FORMAT.format(userBalance) + " 🪙**", true);
        if (isFreeSpin && persistentMultiplier > 1) {
            eb.addField("Sticky Multiplier", "🔥 **x" + persistentMultiplier + "**", true);
        }

        String footerText = user != null ? user.getEffectiveName() + " playing Megaslots" : "Calefaction Megaslots";
        String avatarUrl = user != null ? user.getEffectiveAvatarUrl() : null;
        eb.setFooter(footerText + " • 243 Ways to Win", avatarUrl);

        return eb.build();
    }

    public static MessageEmbed buildReadyEmbed(long betAmount, long userBalance, User user) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("⚡ Super Megaslots — 243 Ways ⚡");
        eb.setColor(new Color(52, 152, 219)); // Vibrant Blue

        String idleGrid = "╔═══════════════════════════╗\n"
                + "║ 👑 │ 💎 │ 7️⃣ │ 🔥 │ 🔔 ║\n"
                + "║ 🍇 │ 🍀 │ 🃏 │ ⭐ │ 💥 ║\n"
                + "║ 🍒 │ 🍋 │ 💎 │ 👑 │ 7️⃣ ║\n"
                + "╚═══════════════════════════╝";

        eb.setDescription("```\n" + idleGrid + "\n```\n"
                + "⚡ **Match symbols left-to-right across 243 ways!**\n"
                + "• 🃏 **Wilds** substitute for any paying symbol\n"
                + "• 💥 **Multiplier Bombs** boost total spin wins by up to 100x\n"
                + "• ⭐ **3+ Scatters** trigger **10 Super Free Spins** with Sticky Multipliers!\n\n"
                + "👉 *Select your bet below and hit Spin to play!*");

        eb.addField("Bet", "🪙 **" + NUMBER_FORMAT.format(betAmount) + "**", true);
        eb.addField("Bankroll", "💰 **" + NUMBER_FORMAT.format(userBalance) + " 🪙**", true);
        eb.setFooter(user != null ? user.getEffectiveName() + " • Ready to Spin" : "Calefaction Megaslots");

        return eb.build();
    }

    public static MessageEmbed buildBonusIntroEmbed(long betAmount, long userBalance, User user, int totalFreeSpins) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🌟 ⚡ SUPER FREE SPINS UNLOCKED! ⚡ 🌟");
        eb.setColor(new Color(155, 89, 182)); // Majestic Purple

        eb.setDescription("⭐ **3+ SCATTERS HIT!** ⭐\n\n"
                + "You've unlocked **" + totalFreeSpins + " Super Free Spins**!\n\n"
                + "🔥 **FEATURE RULES:**\n"
                + "• All Multiplier Bombs (💥) are **STICKY** and accumulate across all spins!\n"
                + "• Your global multiplier will grow with every bomb that lands!\n"
                + "• Every winning combination is boosted by your total sticky multiplier!\n\n"
                + "🚀 *Spins starting now... Hold on to your coins!*");

        eb.addField("Bet Level", "🪙 **" + NUMBER_FORMAT.format(betAmount) + "**", true);
        eb.addField("Bankroll", "💰 **" + NUMBER_FORMAT.format(userBalance) + " 🪙**", true);
        eb.setFooter("BONUS ROUND LAUNCH • 10 Free Spins Incoming!");

        return eb.build();
    }

    public static MessageEmbed buildBonusSummaryEmbed(
            long betAmount,
            long totalBonusWin,
            long userBalance,
            User user,
            int totalSpins,
            int winningSpins,
            long biggestSpin,
            int peakMultiplier
    ) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🏆 🌟 BONUS ROUND COMPLETE! 🌟 🏆");
        eb.setColor(new Color(255, 215, 0)); // Pure Gold

        double xBet = betAmount > 0 ? (double) totalBonusWin / betAmount : 0;
        eb.setDescription("### 🎉 Total Bonus Payout: **" + NUMBER_FORMAT.format(totalBonusWin) + " 🪙** (" + String.format(Locale.US, "%.1fx bet", xBet) + ")!\n\n"
                + "📊 **Bonus Round Recap:**\n"
                + "• **Spins Played:** " + totalSpins + "\n"
                + "• **Winning Spins:** " + winningSpins + " / " + totalSpins + "\n"
                + "• **Peak Sticky Multiplier:** 🔥 **x" + peakMultiplier + "**\n"
                + "• **Biggest Single Spin:** 💰 **" + NUMBER_FORMAT.format(biggestSpin) + " 🪙**\n\n"
                + "💰 *All winnings have been credited to your wallet!*");

        eb.addField("Bet", "🪙 **" + NUMBER_FORMAT.format(betAmount) + "**", true);
        eb.addField("Updated Bankroll", "💰 **" + NUMBER_FORMAT.format(userBalance) + " 🪙**", true);
        eb.setFooter(user != null ? user.getEffectiveName() + " • Bonus Round Champion" : "Calefaction Megaslots");

        return eb.build();
    }

    public static List<ActionRow> buildActionRows(String userId, long betAmount, boolean inFreeSpins) {
        if (inFreeSpins) {
            return List.of(ActionRow.of(
                    Button.secondary("megaslots:disabled", "⚡ Super Free Spins in progress...").asDisabled()
            ));
        }

        long bonusBuyCost = betAmount * 50L;
        ActionRow controls = ActionRow.of(
                Button.primary("megaslots:spin:" + userId + ":" + betAmount, "🔄 Spin (" + NUMBER_FORMAT.format(betAmount) + " 🪙)"),
                Button.danger("megaslots:buy:" + userId + ":" + betAmount, "⚡ Buy Bonus (" + NUMBER_FORMAT.format(bonusBuyCost) + ")"),
                Button.secondary("megaslots:wallet:" + userId + ":" + betAmount, "💰 Wallet")
        );

        long[] betLadder = {50L, 100L, 250L, 500L, 1000L};
        List<Button> betButtons = new ArrayList<>();
        for (long b : betLadder) {
            String label = NUMBER_FORMAT.format(b) + " 🪙";
            if (b == betAmount) {
                betButtons.add(Button.success("megaslots:bet:" + userId + ":" + b, "✔ " + label));
            } else {
                betButtons.add(Button.secondary("megaslots:bet:" + userId + ":" + b, label));
            }
        }

        return List.of(controls, ActionRow.of(betButtons));
    }

    public static ActionRow buildActionRow(String userId, long betAmount, boolean inFreeSpins) {
        return buildActionRows(userId, betAmount, inFreeSpins).get(0);
    }
}
