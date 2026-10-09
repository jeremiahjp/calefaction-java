package com.calefaction.features.games.contexto;

import com.calefaction.features.games.contexto.model.ContextoGuess;
import com.calefaction.features.games.contexto.model.ContextoSession;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

public class ContextoRenderer {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    public static MessageEmbed buildBoardEmbed(ContextoSession session, User user) {
        EmbedBuilder eb = new EmbedBuilder();

        String modeTag = session.isDaily() ? "Daily" : "Unlimited";
        eb.setTitle(String.format("🧩 Contexto #%d (%s)", session.getGameId(), modeTag));

        // Color determination
        if (session.isWon()) {
            eb.setColor(new Color(46, 204, 113)); // Emerald green
        } else if (session.isGivenUp()) {
            eb.setColor(new Color(127, 140, 141)); // Gray
        } else {
            OptionalInt best = session.getBestDistance();
            if (best.isPresent() && best.getAsInt() <= 300) {
                eb.setColor(new Color(46, 204, 113));
            } else if (best.isPresent() && best.getAsInt() <= 1500) {
                eb.setColor(new Color(241, 196, 15));
            } else {
                eb.setColor(new Color(52, 152, 219));
            }
        }

        // Main description / headline
        StringBuilder desc = new StringBuilder();
        if (session.isWon()) {
            String wordPart = session.getSecretWord() != null ? " The secret word was **" + session.getSecretWord() + "**!" : "";
            desc.append(String.format("🎉 **You won!**%s 🎉\nFound in **%d** guesses.\n*Next puzzle available at midnight UTC.*\n\n*💡 Share to another channel or server with `/contexto share [channel]`*",
                    wordPart, session.getTotalGuesses()));
        } else if (session.isGivenUp()) {
            desc.append(String.format("🏳️ **Game Over!** The secret word was **%s**.\n*Next puzzle available at midnight UTC.*\n\n*💡 Share to another channel or server with `/contexto share [channel]`*",
                    session.getSecretWord()));
        } else if (session.getStatusMessage() != null) {
            desc.append(session.getStatusMessage());
        } else {
            desc.append("Guess the secret word! Words are ranked by artificial intelligence based on semantic context.");
        }
        eb.setDescription(desc.toString());

        // Stats Field
        String bestDistStr;
        if (session.getBestDistance().isPresent()) {
            bestDistStr = "#" + NUMBER_FORMAT.format(Math.max(1, session.getBestDistance().getAsInt()));
        } else if (session.isWon()) {
            bestDistStr = "#1";
        } else {
            bestDistStr = "None";
        }
        eb.addField("📊 Stats", String.format("Guesses: **%d** | Hints: **%d** | Closest: **%s**",
                session.getTotalGuesses(), session.getHintsUsed(), bestDistStr), false);

        // Last Guess Field
        ContextoGuess last = session.getLastGuess();
        if (last != null) {
            String badge = getBadge(last.distance());
            String hintTag = last.isHint() ? " *(hint)*" : "";
            eb.addField("🎯 Last Guess", String.format("%s **%s** — #%s%s",
                    badge, last.word(), NUMBER_FORMAT.format(last.distance()), hintTag), false);
        }

        // Top Guesses Leaderboard
        List<ContextoGuess> closest = session.getClosestGuesses(10);
        if (closest.isEmpty()) {
            if (session.isWon()) {
                eb.addField("🏆 Puzzle Completed", String.format("Solved in **%d** guesses! Click **Share Here** below to post your scorecard.", session.getTotalGuesses()), false);
            } else {
                eb.addField("🏆 Closest Guesses", "*No guesses yet. Click **Guess** below to start!*", false);
            }
        } else {
            StringBuilder leaderboard = new StringBuilder();
            for (int i = 0; i < closest.size(); i++) {
                ContextoGuess g = closest.get(i);
                String hintTag = g.isHint() ? " *(hint)*" : "";
                leaderboard.append(String.format("%s `%2d.` **%s** — #%s%s\n",
                        getBadge(g.distance()),
                        i + 1,
                        g.word(),
                        NUMBER_FORMAT.format(g.distance()),
                        hintTag));
            }
            eb.addField("🏆 Closest Guesses", leaderboard.toString(), false);
        }

        eb.setFooter("🟢 1–300 (Close) | 🟡 301–1500 (Medium) | 🔴 1501+ (Cold)",
                user != null ? user.getEffectiveAvatarUrl() : null);

        return eb.build();
    }

    public static MessageEmbed buildHistoryEmbed(ContextoSession session, User user) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(String.format("📜 Contexto #%d Guess History", session.getGameId()));
        eb.setColor(new Color(52, 152, 219));

        List<ContextoGuess> guesses = session.getGuesses();
        if (guesses.isEmpty()) {
            eb.setDescription("*No guesses made yet.*");
        } else {
            StringBuilder sb = new StringBuilder();
            // Show recent guesses (up to 20)
            int start = Math.max(0, guesses.size() - 20);
            for (int i = start; i < guesses.size(); i++) {
                ContextoGuess g = guesses.get(i);
                String hintTag = g.isHint() ? " *(hint)*" : "";
                sb.append(String.format("%s `%d.` **%s** — #%s%s\n",
                        getBadge(g.distance()),
                        i + 1,
                        g.word(),
                        NUMBER_FORMAT.format(g.distance()),
                        hintTag));
            }
            if (start > 0) {
                sb.insert(0, String.format("*... %d earlier guesses not shown*\n\n", start));
            }
            eb.setDescription(sb.toString());
        }

        eb.setFooter(String.format("Total: %d guesses | 🟢 %d | 🟡 %d | 🔴 %d",
                session.getTotalGuesses(), session.getGreenCount(), session.getYellowCount(), session.getRedCount()),
                user != null ? user.getEffectiveAvatarUrl() : null);

        return eb.build();
    }

    public static List<ActionRow> buildActionRows(ContextoSession session, String userId) {
        List<Button> buttons = new ArrayList<>();

        if (!session.isFinished()) {
            buttons.add(Button.primary("contexto:guess:" + userId, "🎯 Guess"));
            buttons.add(Button.secondary("contexto:hint:" + userId, "💡 Hint"));
            buttons.add(Button.secondary("contexto:history:" + userId, "📜 History"));
            buttons.add(Button.danger("contexto:giveup:" + userId, "🏳️ Give Up"));
        } else {
            if (session.hasSharedToChannel(session.getChannelId())) {
                buttons.add(Button.secondary("contexto:share:" + userId, "Shared Here ✅").asDisabled());
            } else {
                buttons.add(Button.success("contexto:share:" + userId, "📢 Share Here"));
            }
            buttons.add(Button.secondary("contexto:history:" + userId, "📜 History"));
        }

        return List.of(ActionRow.of(buttons));
    }

    public static List<ActionRow> buildHistoryActionRows(ContextoSession session, String userId) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.primary("contexto:back:" + userId, "⬅️ Back to Board"));
        if (!session.isFinished()) {
            buttons.add(Button.secondary("contexto:guess:" + userId, "🎯 Guess"));
        }
        return List.of(ActionRow.of(buttons));
    }

    public static List<ActionRow> buildScorecardActionRows() {
        return List.of(ActionRow.of(
                Button.primary("contexto:launch:shared", "🧩 Play Contexto")
        ));
    }

    public static List<ActionRow> buildGiveUpConfirmActionRows(String userId) {
        return List.of(ActionRow.of(
                Button.danger("contexto:confirm_giveup:" + userId, "⚠️ Yes, Give Up"),
                Button.secondary("contexto:back:" + userId, "❌ Cancel (Keep Playing)")
        ));
    }

    public static MessageEmbed buildGiveUpConfirmEmbed(ContextoSession session, User user) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(String.format("🏳️ Contexto #%d — Give Up?", session.getGameId()));
        eb.setColor(new Color(231, 76, 60)); // Red / Warning

        StringBuilder desc = new StringBuilder();
        desc.append("### ⚠️ Are you sure you want to give up?\n\n");
        desc.append("If you give up, **the secret word will be revealed** and you will **not** be able to make any more guesses on today's puzzle.\n\n");
        desc.append("• Click **Yes, Give Up** to forfeit and reveal the secret word.\n");
        desc.append("• Click **Cancel (Keep Playing)** to resume guessing.");

        eb.setDescription(desc.toString());

        String bestDistStr = session.getBestDistance().isPresent()
                ? "#" + NUMBER_FORMAT.format(Math.max(1, session.getBestDistance().getAsInt()))
                : "None";
        eb.addField("📊 Current Progress", String.format("Guesses: **%d** | Hints: **%d** | Closest: **%s**",
                session.getTotalGuesses(), session.getHintsUsed(), bestDistStr), false);

        eb.setFooter("Confirmation required to prevent accidental forfeits",
                user != null ? user.getEffectiveAvatarUrl() : null);

        return eb.build();
    }

    public static String buildScorecard(ContextoSession session, User user) {
        StringBuilder sb = new StringBuilder();
        String userMention = user != null ? user.getAsMention() : "Player";

        if (session.isWon()) {
            sb.append(String.format("🧩 **Contexto #%d**\n", session.getGameId()));
            sb.append(String.format("%s found the secret word in **%d** guesses!\n", userMention, session.getTotalGuesses()));
            if (!session.getGuesses().isEmpty()) {
                sb.append(String.format("🟢 %d  🟡 %d  🔴 %d\n", session.getGreenCount(), session.getYellowCount(), session.getRedCount()));
            }
            if (session.getHintsUsed() > 0) {
                sb.append(String.format("💡 Hints used: %d\n", session.getHintsUsed()));
            }
        } else {
            sb.append(String.format("🧩 **Contexto #%d**\n", session.getGameId()));
            sb.append(String.format("%s gave up after **%d** guesses.\n", userMention, session.getTotalGuesses()));
            if (!session.getGuesses().isEmpty()) {
                sb.append(String.format("🟢 %d  🟡 %d  🔴 %d\n", session.getGreenCount(), session.getYellowCount(), session.getRedCount()));
            }
        }

        return sb.toString();
    }

    public static MessageEmbed buildScorecardEmbed(ContextoSession session, User user) {
        EmbedBuilder eb = new EmbedBuilder();

        String userTag = user != null ? user.getEffectiveName() : "Player";
        String userMention = user != null ? user.getAsMention() : "Player";
        String avatarUrl = user != null ? user.getEffectiveAvatarUrl() : null;

        eb.setTitle(String.format("🧩 Contexto #%d", session.getGameId()));

        if (session.isWon()) {
            eb.setColor(new Color(46, 204, 113)); // Emerald green
            eb.setDescription(String.format(
                    "%s found the secret word in **%d** guesses! 🎉", userMention, session.getTotalGuesses()));
        } else {
            eb.setColor(new Color(231, 76, 60)); // Red
            eb.setDescription(String.format(
                    "%s gave up after **%d** guesses.", userMention, session.getTotalGuesses()));
        }

        // Guess breakdown field
        if (!session.getGuesses().isEmpty()) {
            String breakdown = String.format(
                    "🟢 **%d** hot   🟡 **%d** warm   🔴 **%d** cold",
                    session.getGreenCount(), session.getYellowCount(), session.getRedCount());
            eb.addField("Guesses", breakdown, false);
        } else {
            eb.addField("Score", String.format("Solved in **%d** guesses! 🏆", session.getTotalGuesses()), false);
        }

        // Hints field (only shown if hints were used)
        if (session.getHintsUsed() > 0) {
            eb.addField("Hints used", String.valueOf(session.getHintsUsed()), true);
        }

        if (avatarUrl != null) {
            eb.setFooter(userTag + " played Contexto", avatarUrl);
        } else {
            eb.setFooter(userTag + " played Contexto");
        }

        return eb.build();
    }

    public static String buildVictoryAnnouncement(ContextoSession session, User user) {
        String mention = user != null ? user.getAsMention() : "A player";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎉 %s just solved **Contexto #%d** in **%d** guesses!\n",
                mention, session.getGameId(), session.getTotalGuesses()));
        sb.append(String.format("🟢 %d  🟡 %d  🔴 %d",
                session.getGreenCount(), session.getYellowCount(), session.getRedCount()));
        if (session.getHintsUsed() > 0) {
            sb.append(String.format(" | 💡 Hints: %d", session.getHintsUsed()));
        }
        return sb.toString();
    }

    public static MessageEmbed buildLeaderboardEmbed(int gameId, List<ContextoLeaderboardService.LeaderboardEntry> entries, boolean isDaily) {
        EmbedBuilder eb = new EmbedBuilder();
        String mode = isDaily ? "Daily" : "Puzzle";
        eb.setTitle(String.format("🏆 Contexto #%d %s Leaderboard", gameId, mode));
        eb.setColor(new Color(241, 196, 15)); // Gold

        if (entries.isEmpty()) {
            eb.setDescription("No one has solved this puzzle yet! Run `/contexto play` to be the first on the board.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < entries.size() && i < 10; i++) {
                var entry = entries.get(i);
                String medal;
                if (i == 0) medal = "🥇";
                else if (i == 1) medal = "🥈";
                else if (i == 2) medal = "🥉";
                else medal = String.format("`%2d.`", i + 1);

                String hintsStr = entry.hints() > 0
                        ? String.format(" *(%d %s)*", entry.hints(), entry.hints() == 1 ? "hint" : "hints")
                        : "";
                sb.append(String.format("%s <@%s> — **%d** guesses%s\n",
                        medal, entry.userId(), entry.guesses(), hintsStr));
            }
            eb.setDescription(sb.toString());
        }

        eb.setFooter("Rankings ordered by fewest guesses, then fewest hints.");
        return eb.build();
    }

    public static String getBadge(int distance) {
        if (distance == 0 || distance == 1) {
            return "🎉";
        } else if (distance <= 300) {
            return "🟢";
        } else if (distance <= 1500) {
            return "🟡";
        } else {
            return "🔴";
        }
    }

    public static MessageEmbed buildAdminPanelEmbed(
            int gameId,
            User targetUser,
            String targetUserId,
            ContextoSession session,
            ContextoLeaderboardService.LeaderboardEntry leaderboardEntry,
            String statusNotice
    ) {
        return buildAdminPanelEmbed(gameId, targetUser, targetUserId, session, leaderboardEntry, statusNotice, null);
    }

    public static MessageEmbed buildAdminPanelEmbed(
            int gameId,
            User targetUser,
            String targetUserId,
            ContextoSession session,
            ContextoLeaderboardService.LeaderboardEntry leaderboardEntry,
            String statusNotice,
            java.time.ZoneId zoneId
    ) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(String.format("🛠️ Contexto Admin Panel — Puzzle #%d", gameId));
        eb.setColor(new Color(52, 73, 94)); // Slate admin color

        StringBuilder desc = new StringBuilder();
        if (statusNotice != null && !statusNotice.isBlank()) {
            desc.append(statusNotice).append("\n\n");
        }
        String targetMention = targetUser != null ? targetUser.getAsMention() : "<@" + targetUserId + ">";
        String targetTag = targetUser != null ? targetUser.getEffectiveName() : targetUserId;
        desc.append(String.format("Managing player: %s (`%s`)\nSelect any player from the dropdown below to inspect or reset them.", targetMention, targetTag));
        eb.setDescription(desc.toString());

        // Session status
        if (session != null) {
            String stateTag;
            if (session.isWon()) {
                stateTag = "🏆 Solved";
            } else if (session.isGivenUp()) {
                stateTag = "🏳️ Given Up";
            } else {
                stateTag = "⏳ In Progress";
            }

            String bestStr = session.getBestDistance().isPresent()
                    ? "#" + NUMBER_FORMAT.format(Math.max(1, session.getBestDistance().getAsInt()))
                    : (session.isWon() ? "#1" : "None");

            StringBuilder sessionInfo = new StringBuilder();
            sessionInfo.append(String.format("• **State:** %s\n", stateTag));
            sessionInfo.append(String.format("• **Guesses:** %d", session.getTotalGuesses()));
            if (!session.getGuesses().isEmpty()) {
                sessionInfo.append(String.format(" (🟢 %d  🟡 %d  🔴 %d)", session.getGreenCount(), session.getYellowCount(), session.getRedCount()));
            }
            sessionInfo.append("\n");
            sessionInfo.append(String.format("• **Hints Used:** %d\n", session.getHintsUsed()));
            sessionInfo.append(String.format("• **Best Guess:** %s\n", bestStr));
            if (session.getSecretWord() != null) {
                sessionInfo.append(String.format("• **Secret Word:** `%s`\n", session.getSecretWord()));
            }
            if (session.getLastGuess() != null) {
                sessionInfo.append(String.format("• **Last Guess:** `%s` (#%s)\n", session.getLastGuess().word(), NUMBER_FORMAT.format(session.getLastGuess().distance())));
            }

            eb.addField("🎮 Active Game Session", sessionInfo.toString(), false);
        } else {
            eb.addField("🎮 Active Game Session", "*No active session found for today's puzzle.*", false);
        }

        // Leaderboard status
        if (leaderboardEntry != null) {
            String timeStr = "<t:" + leaderboardEntry.completedAt().getEpochSecond() + ":R>";
            eb.addField("🏆 Leaderboard Record",
                    String.format("• **Result:** Solved in **%d** guesses\n• **Hints:** %d\n• **Finished:** %s",
                            leaderboardEntry.guesses(), leaderboardEntry.hints(), timeStr),
                    false);
        } else {
            eb.addField("🏆 Leaderboard Record", "*No completed leaderboard record for this puzzle.*", false);
        }

        String zoneStr = zoneId != null ? zoneId.getId() : "America/Chicago";
        eb.setFooter(String.format("Contexto Developer Tools • Daily Rollover: %s", zoneStr), targetUser != null ? targetUser.getEffectiveAvatarUrl() : null);
        return eb.build();
    }

    public static List<ActionRow> buildAdminActionRows(String targetUserId, boolean isSelf) {
        EntitySelectMenu userSelect = EntitySelectMenu.create("contexto:admin:select_user", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("🔍 Select a player to inspect or reset...")
                .build();

        List<Button> buttons = new ArrayList<>();
        String resetLabel = isSelf ? "🔄 Reset Myself" : "🔄 Reset Player";
        buttons.add(Button.danger("contexto:admin:reset:" + targetUserId, resetLabel));
        buttons.add(Button.secondary("contexto:admin:inspect:" + targetUserId, "🔍 Inspect Guesses"));
        if (!isSelf) {
            buttons.add(Button.secondary("contexto:admin:reset_self", "🔄 Reset Myself"));
        }
        buttons.add(Button.secondary("contexto:admin:close", "❌ Close"));

        return List.of(ActionRow.of(userSelect), ActionRow.of(buttons));
    }

    public static MessageEmbed buildResetConfirmEmbed(
            int gameId,
            User targetUser,
            String targetUserId,
            ContextoSession session,
            boolean isSelf
    ) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(String.format("⚠️ Contexto #%d — Confirm Reset", gameId));
        eb.setColor(new Color(231, 76, 60)); // Red / Warning

        String targetMention = targetUser != null ? targetUser.getAsMention() : "<@" + targetUserId + ">";
        String who = isSelf ? "your own session" : targetMention;

        StringBuilder desc = new StringBuilder();
        desc.append(String.format("### ⚠️ Are you sure you want to reset %s?\n\n", who));
        desc.append("This action is **irreversible** and will permanently:\n");
        desc.append("• Wipe all active guesses, hints, and session state for today's puzzle\n");
        desc.append("• Remove any victory score from the server leaderboard\n");
        desc.append("• Clear the game start announcement status\n\n");
        desc.append("• Click **Yes, Reset** to permanently wipe this session.\n");
        desc.append("• Click **Cancel** to abort and keep all progress intact.");
        eb.setDescription(desc.toString());

        if (session != null) {
            String stateTag = session.isWon() ? "🏆 Solved" : (session.isGivenUp() ? "🏳️ Given Up" : "⏳ In Progress");
            String bestStr = session.getBestDistance().isPresent()
                    ? "#" + NUMBER_FORMAT.format(Math.max(1, session.getBestDistance().getAsInt()))
                    : (session.isWon() ? "#1" : "None");
            eb.addField("📊 Current Progress to be Wiped",
                    String.format("• **State:** %s\n• **Guesses:** %d | **Hints:** %d | **Closest:** %s",
                            stateTag, session.getTotalGuesses(), session.getHintsUsed(), bestStr),
                    false);
        } else {
            eb.addField("📊 Current Progress", "*No active in-memory session found (only leaderboard / start flag will be cleared if present).*", false);
        }

        eb.setFooter("Confirmation required to prevent accidental data loss",
                targetUser != null ? targetUser.getEffectiveAvatarUrl() : null);

        return eb.build();
    }

    public static List<ActionRow> buildResetConfirmActionRows(String targetUserId, boolean isSelf) {
        String confirmLabel = isSelf ? "⚠️ Yes, Reset Myself" : "⚠️ Yes, Reset Player";
        return List.of(ActionRow.of(
                Button.danger("contexto:admin:confirm_reset:" + targetUserId, confirmLabel),
                Button.secondary("contexto:admin:cancel_reset:" + targetUserId, "❌ Cancel")
        ));
    }
}
