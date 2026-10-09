package com.calefaction.features.games.contexto;

import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Automatically cleans up older Contexto /launch messages, ended game cards,
 * and invite messages when a new session is launched in a channel.
 * Keeps the channel clean without walls of duplicate "Game ended" cards.
 */
@Component
public class ContextoJanitorService extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ContextoJanitorService.class);
    public static final long CONTEXTO_APP_ID = 268171740855664641L;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message newMessage = event.getMessage();

        // Only trigger cleanup when a Contexto launch or invite message is received
        if (!isContextoMessage(newMessage, event.getJDA().getSelfUser().getIdLong())) {
            return;
        }

        MessageChannel channel = event.getChannel();
        long currentMessageId = newMessage.getIdLong();

        boolean canManageMessages = false;
        if (channel instanceof GuildMessageChannel gmc) {
            canManageMessages = gmc.getGuild().getSelfMember().hasPermission(gmc, Permission.MESSAGE_MANAGE);
        }

        final boolean hasManagePermission = canManageMessages;
        final long selfUserId = event.getJDA().getSelfUser().getIdLong();

        // Retrieve the last 25 messages before the current one to find older Contexto messages
        channel.getHistoryBefore(currentMessageId, 25).queue(history -> {
            List<Message> older = history.getRetrievedHistory();
            for (Message oldMsg : older) {
                if (isContextoMessage(oldMsg, selfUserId)) {
                    boolean isOwnMessage = oldMsg.getAuthor().getIdLong() == selfUserId;
                    if (isOwnMessage || hasManagePermission) {
                        oldMsg.delete().queue(
                                success -> log.info("Contexto Janitor cleaned up older message {} in #{}", oldMsg.getId(), channel.getName()),
                                error -> log.debug("Failed to delete older Contexto message {}: {}", oldMsg.getId(), error.getMessage())
                        );
                    }
                }
            }
        }, error -> log.debug("Could not fetch channel history for cleanup: {}", error.getMessage()));
    }

    public boolean isContextoMessage(Message msg, long selfUserId) {
        if (msg == null) {
            return false;
        }

        // 1. Matched by Discord Application ID
        try {
            if (msg.getApplicationIdLong() == CONTEXTO_APP_ID) {
                return true;
            }
        } catch (Exception ignored) {}

        // 2. Matched by Message Activity application
        try {
            if (msg.getActivity() != null && msg.getActivity().getApplication() != null) {
                if (msg.getActivity().getApplication().getIdLong() == CONTEXTO_APP_ID
                        || "Contexto".equalsIgnoreCase(msg.getActivity().getApplication().getName())) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // 3. Matched by /launch slash command interaction
        try {
            if (msg.getType() == MessageType.SLASH_COMMAND && msg.getInteraction() != null) {
                if ("launch".equalsIgnoreCase(msg.getInteraction().getName())) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // 4. Matched by Bot's own invite link or embed
        try {
            if (msg.getAuthor().getIdLong() == selfUserId) {
                String raw = msg.getContentRaw();
                if (raw != null && (raw.contains(String.valueOf(CONTEXTO_APP_ID)) || raw.contains("/activities/"))) {
                    return true;
                }
                if (msg.getEmbeds().stream().anyMatch(e -> e.getTitle() != null && e.getTitle().contains("Contexto"))) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }
}
