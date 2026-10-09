package com.calefaction.features.games.contexto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.calefaction.config.BotProperties;
import com.calefaction.core.CommandRegistry;
import com.calefaction.features.games.contexto.model.ContextoGuess;
import com.calefaction.features.games.contexto.model.ContextoSession;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class ContextoCommandTest {

    @TempDir
    Path tempDir;

    private CommandRegistry commandRegistry;
    private ContextoGameManager gameManager;
    private ContextoLeaderboardService leaderboardService;
    private ContextoCommand command;

    @BeforeEach
    void setUp() {
        commandRegistry = mock(CommandRegistry.class);
        gameManager = mock(ContextoGameManager.class);
        leaderboardService = mock(ContextoLeaderboardService.class);
        when(gameManager.getDailyGameId()).thenReturn(1458);

        command = new ContextoCommand(
                commandRegistry,
                gameManager,
                leaderboardService,
                JsonMapper.builder().build()
        );
        command.setStartsStorageFile(tempDir.resolve("contexto-starts.json"));
    }

    @Test
    void testCheckAndAnnounceGameStart_FirstTimeSendsMessage() {
        GuildMessageChannel channel = mock(GuildMessageChannel.class);
        when(channel.canTalk()).thenReturn(true);
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage(anyString())).thenReturn(action);

        User user = mock(User.class);
        when(user.getAsMention()).thenReturn("<@12345>");

        ContextoSession session = new ContextoSession("user-1", 1458, true);

        command.checkAndAnnounceGameStart(channel, session, user);

        verify(channel, times(1)).sendMessage("🧩 <@12345> started playing **Contexto #1458**!");
        assertTrue(session.isAnnouncedStart());
        assertTrue(command.getAnnouncedStarts().contains("user-1:1458"));
    }

    @Test
    void testCheckAndAnnounceGameStart_DoesNotSpamWhenLeavingAndComingBack() {
        GuildMessageChannel channel = mock(GuildMessageChannel.class);
        when(channel.canTalk()).thenReturn(true);
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage(anyString())).thenReturn(action);

        User user = mock(User.class);
        when(user.getAsMention()).thenReturn("<@12345>");

        ContextoSession session = new ContextoSession("user-1", 1458, true);

        // First play - should announce
        command.checkAndAnnounceGameStart(channel, session, user);
        verify(channel, times(1)).sendMessage(anyString());

        // Player comes back later (second play of the same session/game) - should NOT announce again
        command.checkAndAnnounceGameStart(channel, session, user);
        verify(channel, times(1)).sendMessage(anyString()); // Still only 1 call
    }

    @Test
    void testCheckAndAnnounceGameStart_DoesNotAnnounceIfGuessesAlreadyMade() {
        GuildMessageChannel channel = mock(GuildMessageChannel.class);
        when(channel.canTalk()).thenReturn(true);

        User user = mock(User.class);
        when(user.getAsMention()).thenReturn("<@12345>");

        ContextoSession session = new ContextoSession("user-1", 1458, true);
        session.addGuess(new ContextoGuess("water", 100, Instant.now(), false));

        command.checkAndAnnounceGameStart(channel, session, user);

        verify(channel, never()).sendMessage(anyString());
        assertTrue(session.isAnnouncedStart());
    }

    @Test
    void testCheckAndAnnounceGameStart_DoesNotAnnounceIfAlreadyWonInLeaderboard() {
        GuildMessageChannel channel = mock(GuildMessageChannel.class);
        when(channel.canTalk()).thenReturn(true);

        User user = mock(User.class);
        when(user.getAsMention()).thenReturn("<@12345>");

        when(leaderboardService.findAnyUserEntry(1458, "user-1"))
                .thenReturn(Optional.of(new ContextoLeaderboardService.LeaderboardEntry("user-1", 10, 0, Instant.now())));

        ContextoSession session = new ContextoSession("user-1", 1458, true);

        command.checkAndAnnounceGameStart(channel, session, user);

        verify(channel, never()).sendMessage(anyString());
        assertTrue(session.isAnnouncedStart());
    }

    @Test
    void testCheckAndAnnounceGameStart_InDM_DoesNotSendMessage_MarksAnnounced() {
        MessageChannel dmChannel = mock(MessageChannel.class);

        User user = mock(User.class);
        ContextoSession session = new ContextoSession("user-1", 1458, true);

        command.checkAndAnnounceGameStart(dmChannel, session, user);

        verify(dmChannel, never()).sendMessage(anyString());
        assertTrue(session.isAnnouncedStart());
        assertTrue(command.getAnnouncedStarts().contains("user-1:1458"));

        // If they then come to a guild channel, it should not announce
        GuildMessageChannel guildChannel = mock(GuildMessageChannel.class);
        when(guildChannel.canTalk()).thenReturn(true);
        command.checkAndAnnounceGameStart(guildChannel, session, user);

        verify(guildChannel, never()).sendMessage(anyString());
    }

    @Test
    void testPersistenceAcrossRestarts() {
        Path storage = tempDir.resolve("contexto-starts.json");
        command.setStartsStorageFile(storage);

        GuildMessageChannel channel = mock(GuildMessageChannel.class);
        when(channel.canTalk()).thenReturn(true);
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage(anyString())).thenReturn(action);

        User user = mock(User.class);
        when(user.getAsMention()).thenReturn("<@12345>");

        ContextoSession session = new ContextoSession("user-1", 1458, true);
        command.checkAndAnnounceGameStart(channel, session, user);

        // New command instance simulating bot restart
        ContextoCommand restartedCommand = new ContextoCommand(
                commandRegistry,
                gameManager,
                leaderboardService,
                JsonMapper.builder().build()
        );
        restartedCommand.setStartsStorageFile(storage);

        assertTrue(restartedCommand.getAnnouncedStarts().contains("user-1:1458"));

        ContextoSession sessionAfterRestart = new ContextoSession("user-1", 1458, true);
        restartedCommand.checkAndAnnounceGameStart(channel, sessionAfterRestart, user);

        // sendMessage was called only once in total across both command instances
        verify(channel, times(1)).sendMessage(anyString());
    }

    @Test
    void testCommandDataIncludesAdminAndReset() {
        var subcommands = ((net.dv8tion.jda.api.interactions.commands.build.SlashCommandData) command.getCommandData()).getSubcommands();
        assertTrue(subcommands.stream().anyMatch(s -> s.getName().equals("play")));
        assertTrue(subcommands.stream().anyMatch(s -> s.getName().equals("leaderboard")));
        assertTrue(subcommands.stream().anyMatch(s -> s.getName().equals("share")));
        assertTrue(subcommands.stream().anyMatch(s -> s.getName().equals("admin")));
        assertTrue(subcommands.stream().anyMatch(s -> s.getName().equals("reset")));
    }

    @Test
    void testIsAuthorizedAdmin() {
        BotProperties botProperties = new BotProperties();
        botProperties.setAdminUserIds(List.of("94220323628523520"));

        ContextoCommand cmdWithProps = new ContextoCommand(
                commandRegistry,
                gameManager,
                leaderboardService,
                botProperties
        );

        User devUser = mock(User.class);
        when(devUser.getId()).thenReturn("94220323628523520");

        User normalUser = mock(User.class);
        when(normalUser.getId()).thenReturn("99999999");

        Member adminMember = mock(Member.class);
        when(adminMember.hasPermission(Permission.ADMINISTRATOR)).thenReturn(true);

        Member regularMember = mock(Member.class);
        when(regularMember.hasPermission(Permission.ADMINISTRATOR)).thenReturn(false);

        // Dev user in adminUserIds is authorized
        assertTrue(cmdWithProps.isAuthorizedAdmin(devUser, regularMember));

        // Normal user with server administrator permission is authorized
        assertTrue(cmdWithProps.isAuthorizedAdmin(normalUser, adminMember));

        // Normal user without server administrator permission is NOT authorized
        assertFalse(cmdWithProps.isAuthorizedAdmin(normalUser, regularMember));
        assertFalse(cmdWithProps.isAuthorizedAdmin(normalUser, null));
        assertFalse(cmdWithProps.isAuthorizedAdmin(null, adminMember));
    }

    @Test
    void testResetPlayer() {
        // Pre-fill an announced start
        command.getAnnouncedStarts(); // loads
        User user = mock(User.class);
        when(user.getAsMention()).thenReturn("<@target-user>");
        GuildMessageChannel channel = mock(GuildMessageChannel.class);
        when(channel.canTalk()).thenReturn(true);
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage(anyString())).thenReturn(action);

        ContextoSession session = new ContextoSession("target-user", 1458, true);
        command.checkAndAnnounceGameStart(channel, session, user);
        assertTrue(command.getAnnouncedStarts().contains("target-user:1458"));

        // Reset player
        command.resetPlayer("target-user");

        // Verify gameManager and leaderboardService were called
        verify(gameManager, times(1)).resetSession("target-user");
        verify(leaderboardService, times(1)).removeUserWin(1458, "target-user");

        // Announced starts set is cleared for this user
        assertFalse(command.getAnnouncedStarts().contains("target-user:1458"));
    }
}
