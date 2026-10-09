package com.calefaction.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction;
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationMap;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Discord PRIMARY_ENTRY_POINT application command (type 4).
 * This entry point allows launching the embedded Activity directly from Discord's
 * App Launcher and posts the native purple Game Invitation card with the Play button.
 */
public class PrimaryEntryPointCommandData implements CommandData {

    private String name;
    private String description;
    private int handler;

    public PrimaryEntryPointCommandData() {
        this("launch", "Launch the Contexto Activity", 2);
    }

    public PrimaryEntryPointCommandData(String name, String description) {
        this(name, description, 2);
    }

    public PrimaryEntryPointCommandData(String name, String description, int handler) {
        this.name = name;
        this.description = description;
        this.handler = handler;
    }

    @NotNull
    @Override
    public DataObject toData() {
        return DataObject.empty()
                .put("name", name)
                .put("description", description)
                .put("type", 4) // PRIMARY_ENTRY_POINT
                .put("handler", handler) // 2 = DISCORD_LAUNCH_ACTIVITY
                .put("integration_types", DataArray.fromCollection(List.of(0, 1)))
                .put("contexts", DataArray.fromCollection(List.of(0, 1, 2)));
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Command.Type getType() {
        return Command.Type.UNKNOWN;
    }

    @NotNull
    @Override
    public DefaultMemberPermissions getDefaultPermissions() {
        return DefaultMemberPermissions.ENABLED;
    }

    @NotNull
    @Override
    public Set<InteractionContextType> getContexts() {
        return Set.of(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL);
    }

    @NotNull
    @Override
    public Set<IntegrationType> getIntegrationTypes() {
        return Set.of(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL);
    }

    @Override
    public boolean isNSFW() {
        return false;
    }

    @NotNull
    @Override
    public LocalizationMap getNameLocalizations() {
        return new LocalizationMap(s -> {});
    }

    @NotNull
    @Override
    public CommandData setLocalizationFunction(LocalizationFunction function) {
        return this;
    }

    @NotNull
    @Override
    public CommandData setName(@NotNull String name) {
        this.name = name;
        return this;
    }

    @NotNull
    @Override
    public CommandData setNameLocalization(@NotNull DiscordLocale locale, @NotNull String name) {
        return this;
    }

    @NotNull
    @Override
    public CommandData setNameLocalizations(@NotNull Map<DiscordLocale, String> map) {
        return this;
    }

    @NotNull
    @Override
    public CommandData setDefaultPermissions(@NotNull DefaultMemberPermissions permissions) {
        return this;
    }

    @NotNull
    @Override
    public CommandData setContexts(@NotNull Collection<InteractionContextType> contexts) {
        return this;
    }

    @NotNull
    @Override
    public CommandData setIntegrationTypes(@NotNull Collection<IntegrationType> types) {
        return this;
    }

    @NotNull
    @Override
    public CommandData setNSFW(boolean nsfw) {
        return this;
    }
}
