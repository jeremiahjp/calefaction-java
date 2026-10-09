package com.calefaction.features.games.contexto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContextoJanitorServiceTest {

    private ContextoJanitorService janitorService;
    private static final long BOT_USER_ID = 999999999L;
    private static final long CONTEXTO_APP_ID = ContextoJanitorService.CONTEXTO_APP_ID;

    @BeforeEach
    void setUp() {
        janitorService = new ContextoJanitorService();
    }

    @Test
    void isContextoMessage_matchesApplicationId() {
        Message msg = mock(Message.class);
        when(msg.getApplicationIdLong()).thenReturn(CONTEXTO_APP_ID);

        assertTrue(janitorService.isContextoMessage(msg, BOT_USER_ID));
    }

    @Test
    void isContextoMessage_matchesLaunchSlashCommand() {
        Message msg = mock(Message.class);
        when(msg.getType()).thenReturn(MessageType.SLASH_COMMAND);
        Message.Interaction interaction = mock(Message.Interaction.class);
        when(interaction.getName()).thenReturn("launch");
        when(msg.getInteraction()).thenReturn(interaction);

        assertTrue(janitorService.isContextoMessage(msg, BOT_USER_ID));
    }

    @Test
    void isContextoMessage_matchesMessageActivity() {
        Message msg = mock(Message.class);
        MessageActivity activity = mock(MessageActivity.class);
        MessageActivity.Application app = mock(MessageActivity.Application.class);
        when(app.getIdLong()).thenReturn(CONTEXTO_APP_ID);
        when(activity.getApplication()).thenReturn(app);
        when(msg.getActivity()).thenReturn(activity);

        assertTrue(janitorService.isContextoMessage(msg, BOT_USER_ID));
    }

    @Test
    void isContextoMessage_matchesBotOwnActivityInvite() {
        Message msg = mock(Message.class);
        User author = mock(User.class);
        when(author.getIdLong()).thenReturn(BOT_USER_ID);
        when(msg.getAuthor()).thenReturn(author);
        when(msg.getContentRaw()).thenReturn("https://discord.com/activities/" + CONTEXTO_APP_ID + "?custom_id=contexto:launch:coop:123");

        assertTrue(janitorService.isContextoMessage(msg, BOT_USER_ID));
    }

    @Test
    void isContextoMessage_matchesBotOwnPartyEmbed() {
        Message msg = mock(Message.class);
        User author = mock(User.class);
        when(author.getIdLong()).thenReturn(BOT_USER_ID);
        when(msg.getAuthor()).thenReturn(author);
        MessageEmbed embed = mock(MessageEmbed.class);
        when(embed.getTitle()).thenReturn("👥 Contexto #1481 — Co-op Party!");
        when(msg.getEmbeds()).thenReturn(List.of(embed));

        assertTrue(janitorService.isContextoMessage(msg, BOT_USER_ID));
    }

    @Test
    void isContextoMessage_ignoresUnrelatedMessage() {
        Message msg = mock(Message.class);
        User author = mock(User.class);
        when(author.getIdLong()).thenReturn(12345L);
        when(msg.getAuthor()).thenReturn(author);
        when(msg.getContentRaw()).thenReturn("Hello world!");
        when(msg.getType()).thenReturn(MessageType.DEFAULT);

        assertFalse(janitorService.isContextoMessage(msg, BOT_USER_ID));
    }
}
