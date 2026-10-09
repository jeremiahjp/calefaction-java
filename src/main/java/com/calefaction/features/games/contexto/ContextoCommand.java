package com.calefaction.features.games.contexto;

import com.calefaction.config.BotProperties;
import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.games.contexto.model.ContextoSession;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ContextoCommand implements SlashCommand {

    private static final Logger log = LoggerFactory.getLogger(ContextoCommand.class);
    private final CommandRegistry commandRegistry;
    private final ContextoGameManager gameManager;
    private final ContextoLeaderboardService leaderboardService;
    private final BotProperties botProperties;
    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;
    private final Set<String> announcedStarts = ConcurrentHashMap.newKeySet();
    private Path startsStorageFile;

    @Autowired
    public ContextoCommand(
            CommandRegistry commandRegistry,
            ContextoGameManager gameManager,
            ContextoLeaderboardService leaderboardService,
            @Autowired(required = false) BotProperties botProperties,
            @Autowired(required = false) ObjectMapper objectMapper,
            @Autowired(required = false) JdbcClient jdbcClient
    ) {
        this.commandRegistry = commandRegistry;
        this.gameManager = gameManager;
        this.leaderboardService = leaderboardService;
        this.botProperties = botProperties != null ? botProperties : new BotProperties();
        this.objectMapper = objectMapper != null ? objectMapper : JsonMapper.builder().build();
        this.jdbcClient = jdbcClient;
    }

    public ContextoCommand(
            CommandRegistry commandRegistry,
            ContextoGameManager gameManager,
            ContextoLeaderboardService leaderboardService
    ) {
        this(commandRegistry, gameManager, leaderboardService, null, null, null);
    }

    public ContextoCommand(
            CommandRegistry commandRegistry,
            ContextoGameManager gameManager,
            ContextoLeaderboardService leaderboardService,
            BotProperties botProperties
    ) {
        this(commandRegistry, gameManager, leaderboardService, botProperties, null, null);
    }

    public ContextoCommand(
            CommandRegistry commandRegistry,
            ContextoGameManager gameManager,
            ContextoLeaderboardService leaderboardService,
            ObjectMapper objectMapper
    ) {
        this(commandRegistry, gameManager, leaderboardService, null, objectMapper, null);
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
        initStartsStorage();
    }

    private void initStartsStorage() {
        if (jdbcClient != null) {
            try {
                var rows = jdbcClient.sql("SELECT start_key FROM contexto_starts")
                        .query((rs, rowNum) -> rs.getString("start_key"))
                        .list();
                announcedStarts.addAll(rows);
                log.info("Loaded {} Contexto started announcements from database", rows.size());
            } catch (Exception e) {
                log.debug("Could not load starts from database: {}", e.getMessage());
            }
        }
        if (startsStorageFile == null) {
            Path haDataDir = Path.of("/data");
            Path baseDir = (Files.exists(haDataDir) && Files.isWritable(haDataDir)) ? haDataDir : Path.of("data");
            try {
                Files.createDirectories(baseDir);
                this.startsStorageFile = baseDir.resolve("contexto-starts.json");
            } catch (Exception e) {
                log.warn("Failed to initialize Contexto starts storage directory: {}", e.getMessage());
                this.startsStorageFile = Path.of("contexto-starts.json");
            }
        }
        loadStartsFromFile();
    }

    private synchronized void persistStart(String startKey) {
        announcedStarts.add(startKey);
        if (jdbcClient != null) {
            try {
                jdbcClient.sql("INSERT INTO contexto_starts (start_key, created_at) VALUES (?, NOW()) ON CONFLICT (start_key) DO NOTHING")
                        .param(startKey)
                        .update();
            } catch (Exception e) {
                log.error("Failed to persist start key to database: {}", e.getMessage());
            }
        }
        saveStartsToFile();
    }

    private synchronized void loadStartsFromFile() {
        if (startsStorageFile != null && Files.exists(startsStorageFile)) {
            try {
                Set<String> loaded = objectMapper.readValue(
                        startsStorageFile,
                        new TypeReference<Set<String>>() {}
                );
                if (loaded != null) {
                    announcedStarts.addAll(loaded);
                    log.info("Loaded {} Contexto started announcements from {}", loaded.size(), startsStorageFile);
                }
            } catch (Exception e) {
                log.warn("Failed to load Contexto starts from {}: {}", startsStorageFile, e.getMessage());
            }
        }
    }

    private synchronized void saveStartsToFile() {
        if (startsStorageFile == null) {
            return;
        }
        try {
            Path tmpFile = Path.of(startsStorageFile.toString() + ".tmp");
            objectMapper.writeValue(tmpFile, announcedStarts);
            try {
                Files.move(tmpFile, startsStorageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                Files.move(tmpFile, startsStorageFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("Failed to save Contexto starts to {}: {}", startsStorageFile, e.getMessage());
        }
    }

    void setStartsStorageFile(Path file) {
        this.startsStorageFile = file;
        loadStartsFromFile();
    }

    Set<String> getAnnouncedStarts() {
        return Set.copyOf(announcedStarts);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("contexto", "Play the Contexto semantic word-guessing game!")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL)
                .addSubcommands(
                        new SubcommandData("play", "Launch the Contexto Activity")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "mode", "Choose mode: 'co-op' (public party) or 'solo' (private sp)", false)
                                                .addChoice("👥 Co-op Party (Public)", "co-op")
                                                .addChoice("👤 Solo (Private)", "solo")),
                        new SubcommandData("coop", "Start a public Co-op Party in this channel for everyone to join!"),
                        new SubcommandData("solo", "Launch your private Contexto game (Solo / Single Player)"),
                        new SubcommandData("leaderboard", "View the daily Contexto leaderboard")
                                .addOption(OptionType.INTEGER, "game", "Specific puzzle number (defaults to today)", false),
                        new SubcommandData("share", "Share your daily Contexto scorecard to a channel")
                                .addOption(OptionType.CHANNEL, "channel", "Specific channel to share to (defaults to current channel)", false),
                        new SubcommandData("admin", "Admin/Dev: Open the interactive Contexto Admin Control Panel")
                                .addOption(OptionType.USER, "user", "Target player to manage (defaults to yourself)", false),
                        new SubcommandData("reset", "Admin/Dev: Reset today's Contexto puzzle session")
                                .addOption(OptionType.USER, "user", "Target player to reset (defaults to yourself)", false)
                );
    }

    @Override
    public String getName() {
        return "contexto";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) {
            sub = "play";
        }
        String userId = event.getUser().getId();

        switch (sub) {
            case "play" -> handlePlay(event);
            case "coop", "party" -> handleCoop(event);
            case "solo", "sp" -> handleSolo(event);
            case "guess" -> handleGuessCommand(event, userId);
            case "hint" -> handleHintCommand(event, userId);
            case "history" -> handleHistoryCommand(event, userId);
            case "leaderboard" -> handleLeaderboardCommand(event);
            case "share" -> handleShareCommand(event, userId);
            case "giveup" -> handleGiveUpCommand(event, userId);
            case "admin" -> handleAdminCommand(event);
            case "reset" -> handleResetCommand(event);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    public void handlePlay(SlashCommandInteractionEvent event) {
        OptionMapping modeOpt = event.getOption("mode");
        String mode = modeOpt != null ? modeOpt.getAsString().toLowerCase().trim() : null;

        if ("co-op".equals(mode) || "coop".equals(mode) || "party".equals(mode)) {
            handleCoop(event);
            return;
        }

        if ("solo".equals(mode) || "sp".equals(mode)) {
            handleSolo(event);
            return;
        }

        int dailyGameId = gameManager.getDailyGameId();
        event.replyEmbeds(new EmbedBuilder()
                .setTitle("🧩 Contexto Daily Puzzle #" + dailyGameId)
                .setDescription("Choose how you want to play today:\n\n"
                        + "• **👤 Solo**: Play privately in your own game (no channel messages).\n"
                        + "• **👥 Co-op Party**: Advertise in this channel so friends can join and solve together!")
                .setColor(0x10b981)
                .build())
                .setComponents(ActionRow.of(
                        Button.primary("contexto:launch:solo", "👤 Play Solo"),
                        Button.success("contexto:start_party:" + event.getUser().getId(), "👥 Start Co-op Party")
                ))
                .setEphemeral(true)
                .queue();
    }

    public void handleCoop(SlashCommandInteractionEvent event) {
        int dailyGameId = gameManager.getDailyGameId();
        User user = event.getUser();
        String channelId = event.getChannel() != null ? event.getChannel().getId() : null;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👥 Contexto #" + dailyGameId + " — Co-op Party!")
                .setDescription("🎮 " + user.getAsMention() + " started a **Co-op Party**!\n\n"
                        + "Jump into the activity and work together to find today's secret word! "
                        + "All guesses, clues, and heat ranks are shared in real time.")
                .setColor(0x5865F2)
                .addField("🧩 Daily Puzzle", "#" + dailyGameId, true)
                .addField("👑 Host", user.getAsMention(), true)
                .addField("🎯 Team Goal", "Reach rank #1 together!", true)
                .setFooter("Click below to jump in • Powered by Contexto Activity", null)
                .setTimestamp(java.time.Instant.now());

        String guildId = event.getGuild() != null ? event.getGuild().getId() : "";
        String coopBtnId = channelId != null ? "contexto:launch:coop:" + channelId : "contexto:launch:coop";

        event.replyEmbeds(embed.build())
                .setComponents(ActionRow.of(
                        Button.primary(coopBtnId, "🧩 Join Co-op Party")
                ))
                .queue(hook -> {
                    if (channelId != null) {
                        notifyWorkerPartyStarted(channelId, guildId, dailyGameId, user);
                    }
                });
    }

    public void handleSolo(SlashCommandInteractionEvent event) {
        int dailyGameId = gameManager.getDailyGameId();
        event.replyEmbeds(new EmbedBuilder()
                .setTitle("🧩 Contexto Daily Puzzle #" + dailyGameId + " (Solo)")
                .setDescription("Click below to open your private Contexto Activity!\n*(Your guesses remain completely secret)*")
                .setColor(0x10b981)
                .build())
                .setComponents(ActionRow.of(Button.primary("contexto:launch:solo", "🚀 Play Solo")))
                .setEphemeral(true)
                .queue();
    }

    public void handlePlay(ButtonInteractionEvent event) {
        String channelId = event.getChannel() != null ? event.getChannel().getId() : null;
        String guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        ContextoSession session = getOrCreatePlaySession(event.getUser().getId(), channelId, guildId);

        checkAndAnnounceGameStart(event.getChannel(), session, event.getUser());

        event.replyEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                .setComponents(ContextoRenderer.buildActionRows(session, event.getUser().getId()))
                .setEphemeral(true)
                .queue();
    }

    void checkAndAnnounceGameStart(MessageChannel channel, ContextoSession session, net.dv8tion.jda.api.entities.User user) {
        if (session == null || user == null) {
            return;
        }

        String startKey = session.getUserId() + ":" + session.getGameId();

        // If already announced, don't spam
        if (session.isAnnouncedStart() || announcedStarts.contains(startKey)) {
            session.setAnnouncedStart(true);
            announcedStarts.add(startKey);
            return;
        }

        // If user already has guesses, or has finished/won, they already started in the past
        if (session.getTotalGuesses() > 0 || session.isFinished()
                || leaderboardService.findAnyUserEntry(session.getGameId(), session.getUserId()).isPresent()) {
            session.setAnnouncedStart(true);
            persistStart(startKey);
            return;
        }

        // Mark as announced
        session.setAnnouncedStart(true);
        persistStart(startKey);

        // Only send public announcement if in a guild channel where the bot can talk
        if (channel instanceof GuildMessageChannel gmc && gmc.canTalk()) {
            channel.sendMessage("🧩 " + user.getAsMention() + " started playing **Contexto #" + session.getGameId() + "**!").queue(
                    null,
                    error -> log.warn("Failed to send Contexto game start announcement to {}: {}", channel.getId(), error.getMessage())
            );
        }
    }

    private ContextoSession getOrCreatePlaySession(String userId, String channelId, String guildId) {
        int dailyGameId = gameManager.getDailyGameId();
        Optional<ContextoSession> existing = gameManager.getSession(userId);
        ContextoSession session;
        if (existing.isPresent() && existing.get().getGameId() == dailyGameId) {
            session = existing.get();
        } else {
            session = gameManager.startDailyGame(userId);
            var entryOpt = leaderboardService.findAnyUserEntry(dailyGameId, userId);
            if (entryOpt.isPresent()) {
                session.restoreWin(entryOpt.get().guesses(), entryOpt.get().hints(), entryOpt.get().completedAt());
            }
        }

        if (channelId != null) {
            session.setChannelId(channelId);
        }
        if (guildId != null) {
            session.setGuildId(guildId);
        }
        return session;
    }

    private void handleGuessCommand(SlashCommandInteractionEvent event, String userId) {
        OptionMapping wordOpt = event.getOption("word");
        if (wordOpt == null) {
            event.reply("Please specify a word to guess.").setEphemeral(true).queue();
            return;
        }

        Optional<ContextoSession> opt = gameManager.getSession(userId);
        if (opt.isEmpty()) {
            event.reply("You don't have an active game! Run `/contexto play` first.").setEphemeral(true).queue();
            return;
        }

        ContextoSession current = opt.get();
        if (event.getChannel() != null) {
            current.setChannelId(event.getChannel().getId());
        }
        if (event.getGuild() != null) {
            current.setGuildId(event.getGuild().getId());
        }

        event.deferReply().setEphemeral(true).queue();
        gameManager.submitGuess(userId, wordOpt.getAsString()).thenAccept(session -> {
            if (session == null) {
                event.getHook().sendMessage("Game session not found.").setEphemeral(true).queue();
            } else {
                recordVictoryIfWon(session);
                event.getHook().editOriginalEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                        .setComponents(ContextoRenderer.buildActionRows(session, userId))
                        .queue();
            }
        });
    }

    private void handleLeaderboardCommand(SlashCommandInteractionEvent event) {
        OptionMapping gameOpt = event.getOption("game");
        int dailyId = gameManager.getDailyGameId();
        int gameId = gameOpt != null ? gameOpt.getAsInt() : dailyId;
        boolean isDaily = (gameId == dailyId);
        String guildId = event.getGuild() != null ? event.getGuild().getId() : null;

        var entries = leaderboardService.getLeaderboard(guildId, gameId);
        event.replyEmbeds(ContextoRenderer.buildLeaderboardEmbed(gameId, entries, isDaily)).queue();
    }

    private void handleShareCommand(SlashCommandInteractionEvent event, String userId) {
        int dailyId = gameManager.getDailyGameId();
        Optional<ContextoSession> opt = gameManager.getSession(userId);
        ContextoSession session;
        if (opt.isPresent() && opt.get().getGameId() == dailyId) {
            session = opt.get();
        } else {
            var anyEntry = leaderboardService.findAnyUserEntry(dailyId, userId);
            if (anyEntry.isPresent()) {
                session = gameManager.startDailyGame(userId);
                session.restoreWin(anyEntry.get().guesses(), anyEntry.get().hints(), anyEntry.get().completedAt());
            } else {
                session = null;
            }
        }

        if (session == null || !session.isFinished()) {
            event.reply("❌ You haven't finished today's Contexto puzzle yet! Complete it first using `/contexto play`.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping channelOpt = event.getOption("channel");
        MessageChannel targetChannel;
        if (channelOpt != null) {
            var channelUnion = channelOpt.getAsChannel();
            if (channelUnion instanceof MessageChannel mc) {
                targetChannel = mc;
            } else {
                event.reply("❌ Please select a valid text channel.")
                        .setEphemeral(true)
                        .queue();
                return;
            }
        } else {
            targetChannel = event.getMessageChannel();
        }

        if (targetChannel instanceof GuildMessageChannel gmc && !gmc.canTalk()) {
            event.reply("❌ I don't have permission to post messages in " + targetChannel.getAsMention() + "! Please ensure I have the **View Channel** and **Send Messages** permissions.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String targetChannelId = targetChannel.getId();
        if (session.hasSharedToChannel(targetChannelId)) {
            event.reply("❌ You have already shared today's scorecard to <#" + targetChannelId + ">!")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        var scorecardEmbed = ContextoRenderer.buildScorecardEmbed(session, event.getUser());
        try {
            targetChannel.sendMessageEmbeds(scorecardEmbed)
                    .setComponents(ContextoRenderer.buildScorecardActionRows())
                    .queue(
                    success -> {
                        session.markSharedToChannel(targetChannelId);
                        if (event.getGuild() != null && session.isWon()) {
                            leaderboardService.recordWin(
                                    event.getGuild().getId(),
                                    session.getGameId(),
                                    userId,
                                    session.getTotalGuesses(),
                                    session.getHintsUsed()
                            );
                        }
                    },
                    error -> log.warn("Failed to share scorecard to {}: {}", targetChannelId, error.getMessage())
            );
            session.markSharedToChannel(targetChannelId);
            gameManager.saveSessionsToFile();
            event.reply("📢 Your scorecard has been shared to <#" + targetChannelId + ">!")
                    .setEphemeral(true)
                    .queue();
        } catch (MissingAccessException e) {
            event.reply("❌ Cannot send messages to " + targetChannel.getAsMention() + ": Missing permission " + e.getPermission() + ". Please check bot channel permissions.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void recordVictoryIfWon(ContextoSession session) {
        if (session != null && session.isWon() && !session.isAnnouncedVictory()) {
            session.setAnnouncedVictory(true);
            if (session.isDaily()) {
                leaderboardService.recordWin(
                        session.getGuildId(),
                        session.getGameId(),
                        session.getUserId(),
                        session.getTotalGuesses(),
                        session.getHintsUsed()
                );
            }
        }
    }

    private void handleHintCommand(SlashCommandInteractionEvent event, String userId) {
        Optional<ContextoSession> opt = gameManager.getSession(userId);
        if (opt.isEmpty()) {
            event.reply("You don't have an active game! Run `/contexto play` first.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        gameManager.requestHint(userId).thenAccept(session -> {
            event.getHook().editOriginalEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                    .setComponents(ContextoRenderer.buildActionRows(session, userId))
                    .queue();
        });
    }

    private void handleHistoryCommand(SlashCommandInteractionEvent event, String userId) {
        Optional<ContextoSession> opt = gameManager.getSession(userId);
        if (opt.isEmpty()) {
            event.reply("You don't have an active game! Run `/contexto play` first.").setEphemeral(true).queue();
            return;
        }

        ContextoSession session = opt.get();
        event.replyEmbeds(ContextoRenderer.buildHistoryEmbed(session, event.getUser()))
                .setComponents(ContextoRenderer.buildHistoryActionRows(session, userId))
                .setEphemeral(true)
                .queue();
    }

    private void handleGiveUpCommand(SlashCommandInteractionEvent event, String userId) {
        Optional<ContextoSession> opt = gameManager.getSession(userId);
        if (opt.isEmpty()) {
            event.reply("You don't have an active game! Run `/contexto play` first.").setEphemeral(true).queue();
            return;
        }

        ContextoSession session = opt.get();
        if (session.isFinished()) {
            event.replyEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                    .setComponents(ContextoRenderer.buildActionRows(session, userId))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.replyEmbeds(ContextoRenderer.buildGiveUpConfirmEmbed(session, event.getUser()))
                .setComponents(ContextoRenderer.buildGiveUpConfirmActionRows(userId))
                .setEphemeral(true)
                .queue();
    }

    public boolean isAuthorizedAdmin(User user, Member member) {
        if (user == null) {
            return false;
        }
        if (botProperties.isAdmin(user.getId())) {
            return true;
        }
        return member != null && member.hasPermission(Permission.ADMINISTRATOR);
    }

    public synchronized void resetPlayer(String targetUserId) {
        int gameId = gameManager.getDailyGameId();
        gameManager.resetSession(targetUserId);
        leaderboardService.removeUserWin(gameId, targetUserId);
        announcedStarts.remove(targetUserId + ":" + gameId);
        announcedStarts.removeIf(k -> k.startsWith(targetUserId + ":") || k.endsWith(":" + targetUserId));
        if (jdbcClient != null) {
            try {
                jdbcClient.sql("DELETE FROM contexto_starts WHERE start_key LIKE ? OR start_key LIKE ?")
                        .params(targetUserId + ":%", "%:" + targetUserId)
                        .update();
            } catch (Exception e) {
                log.error("Failed to delete starts from database for user {}: {}", targetUserId, e.getMessage());
            }
        }
        saveStartsToFile();
        log.info("Completely reset Contexto puzzle #{} for user {}", gameId, targetUserId);
    }

    private void handleAdminCommand(SlashCommandInteractionEvent event) {
        if (!isAuthorizedAdmin(event.getUser(), event.getMember())) {
            event.reply("❌ You do not have permission to access the Contexto Admin Panel.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping userOpt = event.getOption("user");
        User targetUser = userOpt != null ? userOpt.getAsUser() : event.getUser();
        String targetUserId = targetUser.getId();
        boolean isSelf = targetUserId.equals(event.getUser().getId());
        int dailyGameId = gameManager.getDailyGameId();

        Optional<ContextoSession> sessionOpt = gameManager.getSession(targetUserId);
        ContextoSession session = sessionOpt.filter(s -> s.getGameId() == dailyGameId).orElse(null);
        var entry = leaderboardService.findAnyUserEntry(dailyGameId, targetUserId).orElse(null);

        event.replyEmbeds(ContextoRenderer.buildAdminPanelEmbed(dailyGameId, targetUser, targetUserId, session, entry, null, gameManager.getZoneId()))
                .setComponents(ContextoRenderer.buildAdminActionRows(targetUserId, isSelf))
                .setEphemeral(true)
                .queue();
    }

    private void handleResetCommand(SlashCommandInteractionEvent event) {
        if (!isAuthorizedAdmin(event.getUser(), event.getMember())) {
            event.reply("❌ You do not have permission to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping userOpt = event.getOption("user");
        User targetUser = userOpt != null ? userOpt.getAsUser() : event.getUser();
        String targetUserId = targetUser.getId();
        boolean isSelf = targetUserId.equals(event.getUser().getId());
        int dailyGameId = gameManager.getDailyGameId();

        Optional<ContextoSession> sessionOpt = gameManager.getSession(targetUserId);
        ContextoSession session = sessionOpt.filter(s -> s.getGameId() == dailyGameId).orElse(null);

        event.replyEmbeds(ContextoRenderer.buildResetConfirmEmbed(dailyGameId, targetUser, targetUserId, session, isSelf))
                .setComponents(ContextoRenderer.buildResetConfirmActionRows(targetUserId, isSelf))
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        // Format: contexto:<action>:<userId>
        if (parts.length < 2 || !parts[0].equals("contexto")) {
            return;
        }

        String action = parts[1];
        if ("launch".equals(action) || ("play".equals(action) && parts.length > 2 && "shared".equals(parts[2]))) {
            handleLaunchActivity(event);
            return;
        }

        if ("start_party".equals(action)) {
            handleStartPartyButton(event);
            return;
        }

        if ("play".equals(action)) {
            handlePlay(event);
            return;
        }

        if ("admin".equals(action)) {
            if (!isAuthorizedAdmin(event.getUser(), event.getMember())) {
                event.reply("❌ You do not have permission to use the Contexto Admin Panel.").setEphemeral(true).queue();
                return;
            }
            if (parts.length < 3) {
                return;
            }
            String subAction = parts[2];
            int dailyGameId = gameManager.getDailyGameId();

            switch (subAction) {
                case "reset_self" -> {
                    String myId = event.getUser().getId();
                    Optional<ContextoSession> sessionOpt = gameManager.getSession(myId);
                    ContextoSession session = sessionOpt.filter(s -> s.getGameId() == dailyGameId).orElse(null);
                    event.deferEdit().queue();
                    event.getHook().editOriginalEmbeds(ContextoRenderer.buildResetConfirmEmbed(dailyGameId, event.getUser(), myId, session, true))
                            .setComponents(ContextoRenderer.buildResetConfirmActionRows(myId, true))
                            .queue();
                }
                case "reset" -> {
                    if (parts.length < 4) return;
                    String targetUserId = parts[3];
                    boolean isSelf = targetUserId.equals(event.getUser().getId());
                    Optional<ContextoSession> sessionOpt = gameManager.getSession(targetUserId);
                    ContextoSession session = sessionOpt.filter(s -> s.getGameId() == dailyGameId).orElse(null);

                    event.deferEdit().queue();
                    event.getJDA().retrieveUserById(targetUserId).queue(targetUser -> {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildResetConfirmEmbed(dailyGameId, targetUser, targetUserId, session, isSelf))
                                .setComponents(ContextoRenderer.buildResetConfirmActionRows(targetUserId, isSelf))
                                .queue();
                    }, error -> {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildResetConfirmEmbed(dailyGameId, null, targetUserId, session, isSelf))
                                .setComponents(ContextoRenderer.buildResetConfirmActionRows(targetUserId, isSelf))
                                .queue();
                    });
                }
                case "confirm_reset" -> {
                    if (parts.length < 4) return;
                    String targetUserId = parts[3];
                    resetPlayer(targetUserId);
                    boolean isSelf = targetUserId.equals(event.getUser().getId());
                    Optional<ContextoSession> sessionOpt = gameManager.getSession(targetUserId);
                    ContextoSession session = sessionOpt.filter(s -> s.getGameId() == dailyGameId).orElse(null);
                    var entry = leaderboardService.findAnyUserEntry(dailyGameId, targetUserId).orElse(null);

                    event.deferEdit().queue();
                    event.getJDA().retrieveUserById(targetUserId).queue(targetUser -> {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildAdminPanelEmbed(dailyGameId, targetUser, targetUserId, session, entry, "✅ **Successfully reset player " + targetUser.getAsMention() + "!** Progress and leaderboard records have been wiped clean.", gameManager.getZoneId()))
                                .setComponents(ContextoRenderer.buildAdminActionRows(targetUserId, isSelf))
                                .queue();
                    }, error -> {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildAdminPanelEmbed(dailyGameId, null, targetUserId, session, entry, "✅ **Successfully reset player <@" + targetUserId + ">!** Progress and leaderboard records have been wiped clean.", gameManager.getZoneId()))
                                .setComponents(ContextoRenderer.buildAdminActionRows(targetUserId, isSelf))
                                .queue();
                    });
                }
                case "cancel_reset" -> {
                    if (parts.length < 4) return;
                    String targetUserId = parts[3];
                    boolean isSelf = targetUserId.equals(event.getUser().getId());
                    Optional<ContextoSession> sessionOpt = gameManager.getSession(targetUserId);
                    ContextoSession session = sessionOpt.filter(s -> s.getGameId() == dailyGameId).orElse(null);
                    var entry = leaderboardService.findAnyUserEntry(dailyGameId, targetUserId).orElse(null);

                    event.deferEdit().queue();
                    event.getJDA().retrieveUserById(targetUserId).queue(targetUser -> {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildAdminPanelEmbed(dailyGameId, targetUser, targetUserId, session, entry, "ℹ️ Reset was cancelled. Player progress remains unchanged.", gameManager.getZoneId()))
                                .setComponents(ContextoRenderer.buildAdminActionRows(targetUserId, isSelf))
                                .queue();
                    }, error -> {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildAdminPanelEmbed(dailyGameId, null, targetUserId, session, entry, "ℹ️ Reset was cancelled. Player progress remains unchanged.", gameManager.getZoneId()))
                                .setComponents(ContextoRenderer.buildAdminActionRows(targetUserId, isSelf))
                                .queue();
                    });
                }
                case "inspect" -> {
                    if (parts.length < 4) return;
                    String targetUserId = parts[3];
                    Optional<ContextoSession> sessionOpt = gameManager.getSession(targetUserId);
                    if (sessionOpt.isEmpty() || sessionOpt.get().getGameId() != dailyGameId) {
                        event.reply("❌ No active game session found for this user in puzzle #" + dailyGameId + ".").setEphemeral(true).queue();
                        return;
                    }
                    ContextoSession session = sessionOpt.get();
                    event.deferReply().setEphemeral(true).queue(hook -> {
                        event.getJDA().retrieveUserById(targetUserId).queue(
                                targetUser -> hook.editOriginalEmbeds(ContextoRenderer.buildHistoryEmbed(session, targetUser)).queue(),
                                error -> hook.editOriginalEmbeds(ContextoRenderer.buildHistoryEmbed(session, null)).queue()
                        );
                    });
                }
                case "close" -> {
                    event.deferEdit().queue();
                    event.getHook().deleteOriginal().queue(null, err -> {
                        event.getHook().editOriginal("❌ Admin panel closed.")
                                .setEmbeds(List.of())
                                .setComponents(List.of())
                                .queue();
                    });
                }
                default -> event.reply("Unknown admin action.").setEphemeral(true).queue();
            }
            return;
        }

        if (parts.length < 3) {
            return;
        }

        String ownerId = parts[2];

        if (!event.getUser().getId().equals(ownerId)) {
            event.reply("❌ This is not your game session! Run `/contexto play` to start your own.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        switch (action) {
            case "guess" -> {
                TextInput wordInput = TextInput.create("guess_word", TextInputStyle.SHORT)
                        .setPlaceholder("Enter a word (e.g. coffee, ocean, forest)")
                        .setMinLength(1)
                        .setMaxLength(30)
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("contexto:guess_modal:" + ownerId, "Contexto Guess")
                        .addComponents(Label.of("Your Guess", wordInput))
                        .build();

                event.replyModal(modal).queue();
            }
            case "hint" -> {
                event.deferEdit().queue();
                gameManager.requestHint(ownerId).thenAccept(session -> {
                    event.getHook().editOriginalEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                            .setComponents(ContextoRenderer.buildActionRows(session, ownerId))
                            .queue();
                });
            }
            case "history" -> {
                event.deferEdit().queue();
                gameManager.getSession(ownerId).ifPresent(session -> {
                    event.getHook().editOriginalEmbeds(ContextoRenderer.buildHistoryEmbed(session, event.getUser()))
                            .setComponents(ContextoRenderer.buildHistoryActionRows(session, ownerId))
                            .queue();
                });
            }
            case "back" -> {
                event.deferEdit().queue();
                gameManager.getSession(ownerId).ifPresent(session -> {
                    event.getHook().editOriginalEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                            .setComponents(ContextoRenderer.buildActionRows(session, ownerId))
                            .queue();
                });
            }
            case "giveup" -> {
                event.deferEdit().queue();
                gameManager.getSession(ownerId).ifPresent(session -> {
                    if (session.isFinished()) {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                                .setComponents(ContextoRenderer.buildActionRows(session, ownerId))
                                .queue();
                    } else {
                        event.getHook().editOriginalEmbeds(ContextoRenderer.buildGiveUpConfirmEmbed(session, event.getUser()))
                                .setComponents(ContextoRenderer.buildGiveUpConfirmActionRows(ownerId))
                                .queue();
                    }
                });
            }
            case "confirm_giveup" -> {
                event.deferEdit().queue();
                gameManager.giveUp(ownerId).thenAccept(session -> {
                    event.getHook().editOriginalEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                            .setComponents(ContextoRenderer.buildActionRows(session, ownerId))
                            .queue();
                });
            }
            case "share" -> {
                Optional<ContextoSession> opt = gameManager.getSession(ownerId);
                if (opt.isPresent()) {
                    ContextoSession session = opt.get();
                    String channelId = event.getChannel() != null ? event.getChannel().getId() : null;
                    if (channelId == null) {
                        event.reply("Unable to determine channel.").setEphemeral(true).queue();
                        return;
                    }
                    if (session.hasSharedToChannel(channelId)) {
                        event.reply("You have already shared your result to this channel!").setEphemeral(true).queue();
                        return;
                    }

                    MessageChannel channel = event.getChannel();
                    if (channel instanceof GuildMessageChannel gmc && !gmc.canTalk()) {
                        event.reply("❌ I don't have permission to post messages in this channel! Please ensure the bot has the **View Channel** and **Send Messages** permissions, or share to another channel with `/contexto share [channel]`.")
                                .setEphemeral(true)
                                .queue();
                        return;
                    }

                    var scorecardEmbed = ContextoRenderer.buildScorecardEmbed(session, event.getUser());
                    try {
                        if (channel != null) {
                            channel.sendMessageEmbeds(scorecardEmbed)
                                    .setComponents(ContextoRenderer.buildScorecardActionRows())
                                    .queue(
                                    success -> {
                                        session.markSharedToChannel(channelId);
                                        if (event.getGuild() != null && session.isWon()) {
                                            leaderboardService.recordWin(
                                                    event.getGuild().getId(),
                                                    session.getGameId(),
                                                    ownerId,
                                                    session.getTotalGuesses(),
                                                    session.getHintsUsed()
                                            );
                                        }
                                    },
                                    error -> log.warn("Failed to share scorecard via button to {}: {}", channelId, error.getMessage())
                            );
                        }
                        session.markSharedToChannel(channelId);
                        gameManager.saveSessionsToFile();
                        event.editComponents(ContextoRenderer.buildActionRows(session, ownerId)).queue();
                    } catch (MissingAccessException e) {
                        event.reply("❌ Cannot post to this channel: Missing permission " + e.getPermission() + ". Please check bot channel permissions.")
                                .setEphemeral(true)
                                .queue();
                    }
                } else {
                    event.reply("Session not found.").setEphemeral(true).queue();
                }
            }
            default -> event.reply("Unknown action.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onModal(ModalInteractionEvent event) {
        String[] parts = event.getModalId().split(":");
        // Format: contexto:guess_modal:<userId>
        if (parts.length < 3 || !parts[0].equals("contexto")) {
            return;
        }

        String ownerId = parts[2];
        if (!event.getUser().getId().equals(ownerId)) {
            event.reply("❌ This is not your game session!").setEphemeral(true).queue();
            return;
        }

        if ("guess_modal".equals(parts[1])) {
            var wordVal = event.getValue("guess_word");
            if (wordVal == null) {
                event.reply("Invalid input.").setEphemeral(true).queue();
                return;
            }

            String word = wordVal.getAsString();
            gameManager.getSession(ownerId).ifPresent(session -> {
                if (event.getChannel() != null) {
                    session.setChannelId(event.getChannel().getId());
                }
                if (event.getGuild() != null) {
                    session.setGuildId(event.getGuild().getId());
                }
            });

            event.deferEdit().queue();
            gameManager.submitGuess(ownerId, word).thenAccept(session -> {
                if (session != null) {
                    recordVictoryIfWon(session);
                    event.getHook().editOriginalEmbeds(ContextoRenderer.buildBoardEmbed(session, event.getUser()))
                            .setComponents(ContextoRenderer.buildActionRows(session, ownerId))
                            .queue();
                }
            });
        }
    }

    @Override
    public void onEntitySelect(EntitySelectInteractionEvent event) {
        if (!"contexto:admin:select_user".equals(event.getComponentId())) {
            return;
        }

        if (!isAuthorizedAdmin(event.getUser(), event.getMember())) {
            event.reply("❌ You do not have permission to access the Contexto Admin Panel.").setEphemeral(true).queue();
            return;
        }

        List<User> selectedUsers = event.getMentions().getUsers();
        if (selectedUsers.isEmpty()) {
            event.reply("Please select a user.").setEphemeral(true).queue();
            return;
        }

        User targetUser = selectedUsers.get(0);
        String targetUserId = targetUser.getId();
        boolean isSelf = targetUserId.equals(event.getUser().getId());
        int dailyGameId = gameManager.getDailyGameId();

        Optional<ContextoSession> sessionOpt = gameManager.getSession(targetUserId);
        ContextoSession session = sessionOpt.filter(s -> s.getGameId() == dailyGameId).orElse(null);
        var entry = leaderboardService.findAnyUserEntry(dailyGameId, targetUserId).orElse(null);

        event.deferEdit().queue();
        event.getHook().editOriginalEmbeds(ContextoRenderer.buildAdminPanelEmbed(dailyGameId, targetUser, targetUserId, session, entry, null, gameManager.getZoneId()))
                .setComponents(ContextoRenderer.buildAdminActionRows(targetUserId, isSelf))
                .queue();
    }

    private void handleLaunchActivity(ButtonInteractionEvent event) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://discord.com/api/v10/interactions/" + event.getId() + "/" + event.getToken() + "/callback"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"type\":12}"))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Activity launch callback status {}: {}", response.statusCode(), response.body());
                event.reply("Could not launch activity: " + response.body()).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            log.error("Failed to respond with LAUNCH_ACTIVITY: {}", e.getMessage(), e);
            event.reply("Error launching activity: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handleStartPartyButton(ButtonInteractionEvent event) {
        int dailyGameId = gameManager.getDailyGameId();
        User user = event.getUser();
        MessageChannel channel = event.getChannel();

        if (channel instanceof GuildMessageChannel gmc && !gmc.canTalk()) {
            event.reply("❌ I do not have permission to post messages in this channel!").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👥 Contexto #" + dailyGameId + " — Co-op Party!")
                .setDescription("🎮 " + user.getAsMention() + " started a **Co-op Party**!\n\n"
                        + "Jump into the activity and work together to find today's secret word! "
                        + "All guesses, clues, and heat ranks are shared in real time.")
                .setColor(0x5865F2)
                .addField("🧩 Daily Puzzle", "#" + dailyGameId, true)
                .addField("👑 Host", user.getAsMention(), true)
                .addField("🎯 Team Goal", "Reach rank #1 together!", true)
                .setFooter("Click below to jump in • Powered by Contexto Activity", null)
                .setTimestamp(java.time.Instant.now());

        String guildId = event.getGuild() != null ? event.getGuild().getId() : "";
        String channelId = channel != null ? channel.getId() : "";
        String coopCustomId = channelId.isEmpty() ? "contexto:launch:coop" : "contexto:launch:coop:" + channelId;

        if (channel != null) {
            channel.sendMessageEmbeds(embed.build())
                    .setComponents(ActionRow.of(
                            Button.primary(coopCustomId, "🧩 Join Co-op Party")
                    ))
                    .queue(msg -> {
                        notifyWorkerPartyStarted(channelId, guildId, dailyGameId, user);
                    });
        }

        event.editMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Co-op Party Started!")
                .setDescription("Your Co-op Party has been advertised in " + (channel != null ? channel.getAsMention() : "this channel") + "!\nClick below to launch into the party:")
                .setColor(0x10b981)
                .build())
                .setComponents(ActionRow.of(
                        Button.primary(coopCustomId, "🚀 Join Co-op Party")
                ))
                .queue();
    }

    private void notifyWorkerPartyStarted(String channelId, String guildId, int gameId, User user) {
        if (channelId == null || user == null) return;
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String escapedName = user.getEffectiveName().replace("\"", "\\\"");
            String json = String.format(
                    "{\"roomId\":\"channel-%s\",\"guildId\":\"%s\",\"gameId\":%d,\"host\":{\"id\":\"%s\",\"name\":\"%s\"}}",
                    channelId,
                    guildId != null ? guildId : "",
                    gameId,
                    user.getId(),
                    escapedName
            );
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://discord-contexto.i-r-stuff.workers.dev/api/collab/start"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .build();
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to notify worker of co-op party start: {}", e.getMessage());
        }
    }
}
