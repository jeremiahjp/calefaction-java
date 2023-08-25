package com.jp.calefaction.listeners;

import com.jp.calefaction.listeners.buttons.ButtonHandler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ButtonEventListener {
    private final ApplicationContext applicationContext;

    public ButtonEventListener(GatewayDiscordClient client, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    @EventListener
    public Mono<Void> handle(ButtonInteractionEvent event) {
        log.info("inside application context handle");
        Optional<String> interactedCustomId = getButtonLabel(event);

        ButtonHandler handler =
                (ButtonHandler) applicationContext.getBean(interactedCustomId.get()); // TODO: check for NPE

        if (handler != null) {
            return handler.handle(event);
        } else {
            return handleUnknownButton(event);
        }
    }

    private Mono<Void> handleUnknownButton(ButtonInteractionEvent event) {
        return event.reply("Unknown button clicked!");
    }

    private Optional<String> getButtonLabel(ButtonInteractionEvent event) {
        List<LayoutComponent> components = event.getMessage().get().getComponents();

        List<ActionRow> actionRows = components.stream()
                .filter(component -> component instanceof ActionRow)
                .map(component -> (ActionRow) component)
                .collect(Collectors.toList());

        return actionRows.stream()
                .flatMap(actionRow -> actionRow.getChildren().stream())
                .filter(item -> item instanceof Button)
                .map(item -> (Button) item)
                .filter(button -> button.getCustomId().get().equals(event.getCustomId()))
                .findFirst()
                .map(Button::getLabel)
                .get();
    }
}
