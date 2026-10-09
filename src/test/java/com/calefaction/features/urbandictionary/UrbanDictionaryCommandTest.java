package com.calefaction.features.urbandictionary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.calefaction.core.CommandRegistry;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class UrbanDictionaryCommandTest {

    private CommandRegistry commandRegistry;
    private UrbanDictionaryService urbanDictionaryService;
    private UrbanDictionaryCommand command;
    private SlashCommandInteractionEvent event;
    private InteractionHook hook;

    @BeforeEach
    void setUp() {
        commandRegistry = mock(CommandRegistry.class);
        urbanDictionaryService = mock(UrbanDictionaryService.class);
        command = new UrbanDictionaryCommand(commandRegistry, urbanDictionaryService);

        event = mock(SlashCommandInteractionEvent.class);
        hook = mock(InteractionHook.class);
        ReplyCallbackAction deferAction = mock(ReplyCallbackAction.class);

        OptionMapping option = mock(OptionMapping.class);
        when(option.getAsString()).thenReturn("yeet");
        when(event.getOption("term")).thenReturn(option);
        when(event.deferReply()).thenReturn(deferAction);
        when(event.getHook()).thenReturn(hook);
    }

    @Test
    void testExecute_DefinitionFound_PostsOnce() {
        UrbanDictionaryService.Definition def = new UrbanDictionaryService.Definition(
                "yeet",
                "To [throw] something with force",
                "He just [yeet]ed that ball",
                "Author",
                "https://www.urbandictionary.com/define.php?term=yeet",
                100
        );

        when(urbanDictionaryService.define("yeet")).thenReturn(Mono.just(def));

        WebhookMessageCreateAction<net.dv8tion.jda.api.entities.Message> embedAction = mock(WebhookMessageCreateAction.class);
        when(hook.sendMessageEmbeds(any(MessageEmbed.class))).thenReturn(embedAction);

        command.execute(event);

        // Verify embed is sent
        verify(hook, times(1)).sendMessageEmbeds(any(MessageEmbed.class));
        // Verify "Could not find definition" is NOT sent
        verify(hook, never()).sendMessage(anyString());
    }

    @Test
    void testExecute_DefinitionNotFound_PostsNotFoundMessageOnce() {
        when(urbanDictionaryService.define("yeet")).thenReturn(Mono.empty());

        WebhookMessageCreateAction<net.dv8tion.jda.api.entities.Message> stringAction = mock(WebhookMessageCreateAction.class);
        when(hook.sendMessage(anyString())).thenReturn(stringAction);

        command.execute(event);

        // Verify embed is NOT sent
        verify(hook, never()).sendMessageEmbeds(any(MessageEmbed.class));
        // Verify "Could not find definition" message is sent once
        verify(hook, times(1)).sendMessage("Could not find definition for: yeet");
    }
}
