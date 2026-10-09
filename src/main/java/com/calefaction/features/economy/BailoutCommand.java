package com.calefaction.features.economy;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class BailoutCommand implements SlashCommand {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private final CommandRegistry commandRegistry;
    private final EconomyService economyService;

    public BailoutCommand(CommandRegistry commandRegistry, EconomyService economyService) {
        this.commandRegistry = commandRegistry;
        this.economyService = economyService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("bailout", "Claim 1,000 emergency bailout coins if your balance is under 100")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL);
    }

    @Override
    public String getName() {
        return "bailout";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        var result = economyService.claimBailout(userId);

        if (result.success()) {
            var wallet = economyService.getWallet(userId);
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("🚨 Emergency Bailout Approved! 🚨");
            eb.setColor(new Color(46, 204, 113)); // Emerald Green

            eb.setDescription("### 🪙 Granted: **+" + NUMBER_FORMAT.format(result.amountGranted()) + " Coins!**\n"
                    + "Your new balance: **" + NUMBER_FORMAT.format(wallet.balance()) + " 🪙**\n\n"
                    + "👉 *You're back in action! Head over to `/megaslots` to spin!*");

            eb.addField("Updated Bankroll", "💰 **" + NUMBER_FORMAT.format(wallet.balance()) + " 🪙**", true);
            eb.addField("Spins Played", "🎰 **" + NUMBER_FORMAT.format(wallet.spinsPlayed()) + "**", true);
            eb.setFooter(event.getUser().getEffectiveName() + " • Emergency Reload", event.getUser().getEffectiveAvatarUrl());

            event.replyEmbeds(eb.build())
                    .setComponents(EconomyCommand.buildActionRow(userId, wallet.balance()))
                    .queue();
        } else {
            event.reply(result.message())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
