package com.calefaction.features.economy;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.text.NumberFormat;
import java.util.Locale;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class DailyCommand implements SlashCommand {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private final CommandRegistry commandRegistry;
    private final EconomyService economyService;

    public DailyCommand(CommandRegistry commandRegistry, EconomyService economyService) {
        this.commandRegistry = commandRegistry;
        this.economyService = economyService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("daily", "Claim your free daily coin reward (refreshes every 24h)")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL);
    }

    @Override
    public String getName() {
        return "daily";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        var result = economyService.claimDaily(userId);

        if (result.success()) {
            var wallet = economyService.getWallet(userId);
            event.replyEmbeds(EconomyCommand.buildBalanceEmbed(wallet, event.getUser()))
                    .setComponents(EconomyCommand.buildActionRow(userId, wallet.balance()))
                    .queue();
        } else {
            long hours = result.timeUntilNext().toHours();
            long minutes = result.timeUntilNext().toMinutesPart();
            event.reply("⏳ " + result.message() + " Ready in **" + hours + "h " + minutes + "m**.\nYour current balance: **"
                            + NUMBER_FORMAT.format(economyService.getBalance(userId)) + " 🪙**")
                    .setEphemeral(true)
                    .queue();
        }
    }
}
