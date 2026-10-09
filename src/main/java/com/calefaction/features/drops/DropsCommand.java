package com.calefaction.features.drops;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.drops.model.TweetEntry;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DropsCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final DropNotificationService dropService;
    private final DropProperties dropProperties;

    @Autowired
    public DropsCommand(
            CommandRegistry commandRegistry,
            DropNotificationService dropService,
            DropProperties dropProperties
    ) {
        this.commandRegistry = commandRegistry;
        this.dropService = dropService;
        this.dropProperties = dropProperties;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("drops", "Pokémon TCG Drop Monitor & Twitter Alerts")
                .addSubcommands(
                        new SubcommandData("status", "View current drop monitor configuration & statistics"),
                        new SubcommandData("check", "Force an immediate live check of @Drop_Notify and @PokemonDealsTCG"),
                        new SubcommandData("test", "Send a test drop alert to verify embed layout, buttons, and pings")
                )
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM, InteractionContextType.PRIVATE_CHANNEL);
    }

    @Override
    public String getName() {
        return "drops";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            handleStatus(event);
            return;
        }

        switch (subcommand) {
            case "status" -> handleStatus(event);
            case "check" -> handleCheck(event);
            case "test" -> handleTest(event);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DropRenderer.POKEMON_YELLOW);
        eb.setTitle("⚡ Pokémon Drop Monitor Status");

        eb.addField("Status", dropProperties.isEnabled() ? "🟢 Enabled" : "🔴 Disabled", true);
        eb.addField("Poll Interval", dropProperties.getPollIntervalSeconds() + " seconds", true);

        Instant lastPoll = dropService.getLastPollTime();
        eb.addField("Last Poll", lastPoll != null ? "<t:" + lastPoll.getEpochSecond() + ":R>" : "Not yet polled", true);

        List<String> channels = dropProperties.getChannelIds();
        String channelStr = channels.isEmpty() ? "*None configured* (set `drops.channel-ids`)" :
                channels.stream().map(id -> "<#" + id + ">").reduce((a, b) -> a + ", " + b).orElse("");
        eb.addField("Alert Channels", channelStr, false);

        List<String> users = dropProperties.getMentionUserIds();
        String userStr = users.isEmpty() ? "*None*" :
                users.stream().map(id -> "<@" + id + ">").reduce((a, b) -> a + ", " + b).orElse("");
        eb.addField("Mentioned Users", userStr, true);

        List<String> roles = dropProperties.getMentionRoleIds();
        String roleStr = roles.isEmpty() ? "*None*" :
                roles.stream().map(id -> "<@&" + id + ">").reduce((a, b) -> a + ", " + b).orElse("");
        eb.addField("Mentioned Roles", roleStr, true);

        eb.addField("Monitored Accounts", "• [@PokemonDealsTCG](https://x.com/PokemonDealsTCG) (All TCG drops)\n• [@Drop_Notify](https://x.com/Drop_Notify) (Filtered by Pokémon keywords)\n• [@jjuddpi](https://x.com/jjuddpi) (Test account - all tweets)", false);

        eb.addField("Stored Seen Tweets", String.valueOf(dropService.getSeenTweetIds().size()), true);

        eb.setFooter("Calefaction Drops Monitor", null);
        eb.setTimestamp(Instant.now());

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private void handleCheck(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        dropService.checkDrops().whenComplete((newDrops, ex) -> {
            if (ex != null) {
                event.getHook().sendMessage("❌ Error checking drops: " + ex.getMessage()).setEphemeral(true).queue();
            } else {
                String msg = String.format("✅ Check complete! Checked @Drop_Notify, @PokemonDealsTCG & @jjuddpi.\n**%d** new drop alert(s) detected.", newDrops.size());
                event.getHook().sendMessage(msg).setEphemeral(true).queue();
            }
        });
    }

    private void handleTest(SlashCommandInteractionEvent event) {
        TweetEntry sampleTweet = new TweetEntry(
                "sample-123456",
                "PokemonDealsTCG",
                "Pokemon Deals, Alerts & News!",
                "https://pbs.twimg.com/profile_images/1890825510713958400/f4XCVYef_bigger.jpg",
                "🚨 IN STOCK NOW: Pokémon TCG Prismatic Evolutions Elite Trainer Box at Amazon!\n\nLink: https://amazon.com/dp/sample\n\nBe fast, expected to sell out in minutes!",
                Instant.now(),
                List.of("https://amazon.com/dp/sample"),
                List.of("https://images.pokemontcg.io/sv8pt5/symbol.png")
        );

        var embed = DropRenderer.buildDropEmbed(sampleTweet, "PRISMATIC EVOLUTIONS");
        var actionRows = DropRenderer.buildActionRows(sampleTweet);
        String mention = DropRenderer.buildMentionString(dropProperties.getMentionUserIds(), dropProperties.getMentionRoleIds());

        var reply = event.replyEmbeds(embed).setComponents(actionRows).setEphemeral(false);
        if (!mention.isBlank()) {
            reply.setContent("🔔 **[TEST ALERT]** " + mention);
        }
        reply.queue();
    }
}
