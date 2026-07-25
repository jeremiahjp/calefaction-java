package com.calefaction.core;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommand {
    CommandData getCommandData();

    String getName();

    void execute(SlashCommandInteractionEvent event);

    default void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
    }

    default void onButton(ButtonInteractionEvent event) {
    }
}
