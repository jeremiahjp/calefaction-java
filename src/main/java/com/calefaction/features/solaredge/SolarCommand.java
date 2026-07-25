package com.calefaction.features.solaredge;

import com.calefaction.config.BotProperties;
import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.solaredge.dto.*;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.time.LocalDate;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

@Component
public class SolarCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final SolarEdgeService solarEdgeService;
    private final BotProperties botProperties;

    public SolarCommand(CommandRegistry commandRegistry, SolarEdgeService solarEdgeService, BotProperties botProperties) {
        this.commandRegistry = commandRegistry;
        this.solarEdgeService = solarEdgeService;
        this.botProperties = botProperties;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("solar", "View SolarEdge inverter status")
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                .setContexts(InteractionContextType.GUILD, InteractionContextType.BOT_DM,
                        InteractionContextType.PRIVATE_CHANNEL)
                .addSubcommands(
                        new SubcommandData("overview", "View general system overview"),
                        new SubcommandData("details", "View site details and installation info"),
                        new SubcommandData("energy", "View energy production stats")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "unit", "Time unit (DAY, MONTH, YEAR)", false)
                                                .addChoice("Daily", "DAY")
                                                .addChoice("Monthly", "MONTH")
                                                .addChoice("Yearly", "YEAR")),
                        new SubcommandData("power", "View power generation graph for a date")
                                .addOptions(new OptionData(OptionType.STRING, "date",
                                        "Date (YYYY-MM-DD), defaults to today", false)),
                        new SubcommandData("flow", "View current power flow (Live status)"),
                        new SubcommandData("inventory", "View site inventory"),
                        new SubcommandData("benefits", "View environmental benefits"));
    }

    @Override
    public String getName() {
        return "solar";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!botProperties.isAdmin(event.getUser().getId())) {
            event.reply("⛔ You are not authorized to use this command.").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null)
            subcommand = "overview";

        event.deferReply().queue();

        switch (subcommand) {
            case "overview" -> handleOverview(event);
            case "details" -> handleDetails(event);
            case "energy" -> handleEnergy(event);
            case "power" -> handlePower(event);
            case "flow" -> handleFlow(event);
            case "inventory" -> handleInventory(event);
            case "benefits" -> handleBenefits(event);
            default -> event.getHook().sendMessage("Unknown subcommand: " + subcommand).queue();
        }
    }

    private void handleOverview(SlashCommandInteractionEvent event) {
        solarEdgeService.getOverview().subscribe(root -> {
            try {
                Overview overview = root.overview();
                double currentPowerKw = overview.currentPower().power() / 1000.0;
                double energyToday = overview.lastDayData().energy() / 1000.0;
                double energyMonth = overview.lastMonthData().energy() / 1000.0;
                double energyYear = overview.lastYearData().energy() / 1000.0;
                double energyLifetime = overview.lifeTimeData().energy() / 1000000.0;

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("☀️ Solar System Overview");
                eb.setColor(Color.ORANGE);
                eb.setDescription(String.format("**Current Output**: %.2f kW", currentPowerKw));
                eb.addField("Today", String.format("%.2f kWh", energyToday), true);
                eb.addField("This Month", String.format("%.2f kWh", energyMonth), true);
                eb.addField("This Year", String.format("%.2f kWh", energyYear), true);
                eb.addField("Lifetime", String.format("%.2f MWh", energyLifetime), false);
                eb.setFooter("SolarEdge Monitoring");

                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                event.getHook().sendMessage("Error parsing overview data: " + e.getMessage()).queue();
            }
        }, error -> event.getHook().sendMessage("Could not fetch SolarEdge data: " + error.getMessage()).queue(),
                () -> event.getHook().sendMessage("Could not fetch SolarEdge data (Empty response).").queue());
    }

    private void handleDetails(SlashCommandInteractionEvent event) {
        solarEdgeService.getDetails().subscribe(root -> {
            try {
                Details details = root.details();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("ℹ️ Site Details: " + details.name());
                eb.setColor(Color.BLUE);
                eb.addField("Status", details.status(), true);
                eb.addField("Installed", details.installationDate(), true);
                eb.addField("Peak Power", details.peakPower() + " kWp", true);
                eb.addField("Location", details.location().city() + ", " + details.location().country(), true);

                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                event.getHook().sendMessage("Error parsing details: " + e.getMessage()).queue();
            }
        }, error -> event.getHook().sendMessage("Could not fetch details: " + error.getMessage()).queue(),
                () -> event.getHook().sendMessage("Could not fetch details (Empty response).").queue());
    }

    private void handleEnergy(SlashCommandInteractionEvent event) {
        String unit = event.getOption("unit") != null ? event.getOption("unit").getAsString() : "DAY";
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);

        solarEdgeService.getEnergy(start.toString(), end.toString(), unit).subscribe(root -> {
            try {
                Energy energy = root.energy();
                List<DateValue> values = energy.values();

                double totalEnergy = values.stream()
                        .filter(v -> v.value() != null)
                        .mapToDouble(DateValue::value)
                        .sum();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("⚡ Energy Production (" + unit + ")");
                eb.setDescription(String.format("Total in period: **%.2f kWh**", totalEnergy / 1000.0));
                eb.setColor(Color.GREEN);

                StringBuilder sb = new StringBuilder();
                int skip = Math.max(0, values.size() - 10);

                for (int i = skip; i < values.size(); i++) {
                    DateValue val = values.get(i);
                    if (val.value() == null)
                        continue;

                    String date = val.date();
                    double kwh = val.value() / 1000.0;
                    sb.append(String.format("`%s`: %.1f kWh\n", date.split(" ")[0], kwh));
                }

                eb.addField("Recent History", sb.toString(), false);
                event.getHook().sendMessageEmbeds(eb.build()).queue();

            } catch (Exception e) {
                event.getHook().sendMessage("Error parsing energy data: " + e.getMessage()).queue();
            }
        }, error -> event.getHook().sendMessage("Could not fetch energy data: " + error.getMessage()).queue(),
                () -> event.getHook().sendMessage("Could not fetch energy data (Empty response).").queue());
    }

    private void handlePower(SlashCommandInteractionEvent event) {
        String dateStr = event.getOption("date") != null ? event.getOption("date").getAsString()
                : LocalDate.now().toString();
        String startTime = dateStr + " 00:00:00";
        String endTime = dateStr + " 23:59:59";

        solarEdgeService.getPower(startTime, endTime).subscribe(root -> {
            try {
                Power power = root.power();
                List<DateValue> values = power.values();

                double maxPower = 0;
                String maxTime = "";

                for (DateValue val : values) {
                    if (val.value() == null)
                        continue;
                    if (val.value() > maxPower) {
                        maxPower = val.value();
                        maxTime = val.date();
                    }
                }

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("📈 Power Curve: " + dateStr);
                eb.setColor(Color.RED);
                if (!maxTime.isEmpty()) {
                    eb.setDescription(
                            String.format("**Peak Power**: %.2f kW at %s", maxPower / 1000.0, maxTime.split(" ")[1]));
                } else {
                    eb.setDescription("No power data available for this date.");
                }

                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                event.getHook().sendMessage("Error parsing power data: " + e.getMessage()).queue();
            }
        }, error -> event.getHook().sendMessage("Could not fetch power data: " + error.getMessage()).queue(),
                () -> event.getHook().sendMessage("Could not fetch power data (Empty response).").queue());
    }

    private void handleFlow(SlashCommandInteractionEvent event) {
        solarEdgeService.getCurrentPowerFlow().subscribe(root -> {
            try {
                PowerFlow flow = root.siteCurrentPowerFlow();
                String unit = flow.unit();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("⚡ Current Power Flow");
                eb.setColor(Color.YELLOW);

                eb.addField("☀️ Solar (PV)", String.format("%.2f %s", flow.PV().currentPower(), unit), true);
                eb.addField("🏠 Load", String.format("%.2f %s", flow.LOAD().currentPower(), unit), true);
                eb.addField("🔌 Grid",
                        String.format("%.2f %s (%s)", flow.GRID().currentPower(), unit, flow.GRID().status()), true);

                if (flow.STORAGE() != null && "Active".equalsIgnoreCase(flow.STORAGE().status())) {
                    eb.addField("🔋 Battery", String.format("%.2f %s (%.0f%%)", flow.STORAGE().currentPower(), unit,
                            flow.STORAGE().chargeLevel()), true);
                }

                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                event.getHook().sendMessage("Error parsing power flow: " + e.getMessage()).queue();
            }
        }, error -> event.getHook().sendMessage("Could not fetch power flow: " + error.getMessage()).queue(),
                () -> event.getHook().sendMessage("Could not fetch power flow (Empty response).").queue());
    }

    private void handleInventory(SlashCommandInteractionEvent event) {
        solarEdgeService.getInventory().subscribe(root -> {
            try {
                Inventory inventory = root.inventory();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("📦 Site Inventory");
                eb.setColor(Color.DARK_GRAY);

                if (inventory.inverters() != null) {
                    for (Inverter inv : inventory.inverters()) {
                        eb.addField("Inverter", "SN: " + inv.SN() + "\nModel: " + inv.model(), false);
                    }
                }
                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                event.getHook().sendMessage("Error parsing inventory: " + e.getMessage()).queue();
            }
        }, error -> event.getHook().sendMessage("Could not fetch inventory: " + error.getMessage()).queue(),
                () -> event.getHook().sendMessage("Could not fetch inventory (Empty response).").queue());
    }

    private void handleBenefits(SlashCommandInteractionEvent event) {
        solarEdgeService.getEnvBenefits().subscribe(root -> {
            try {
                EnvBenefits env = root.envBenefits();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("🌱 Environmental Benefits");
                eb.setColor(Color.GREEN);

                if (env.gasEmissionSaved() != null) {
                    eb.addField("CO₂ Saved",
                            String.format("%.2f %s", env.gasEmissionSaved().co2(), env.gasEmissionSaved().units()),
                            true);
                }
                eb.addField("Trees Planted", String.format("%.2f", env.treesPlanted()), true);
                eb.addField("Light Bulbs", String.format("%.2f", env.lightBulbs()), true);

                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                event.getHook().sendMessage("Error parsing benefits: " + e.getMessage()).queue();
            }
        }, error -> event.getHook().sendMessage("Could not fetch benefits: " + error.getMessage()).queue(),
                () -> event.getHook().sendMessage("Could not fetch benefits (Empty response).").queue());
    }
}
