package com.calefaction.features.system;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class StatusCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;

    public StatusCommand(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("status", "Check system status and resource usage");
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);
        String uptimeStr = String.format("%dd %dh %dm %ds",
                uptime.toDays(),
                uptime.toHoursPart(),
                uptime.toMinutesPart(),
                uptime.toSecondsPart());

        int threadCount = Thread.activeCount();

        com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean();
        double processCpu = osBean.getProcessCpuLoad() * 100;
        double systemCpu = osBean.getCpuLoad() * 100;

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("System Status");
        eb.setColor(Color.GREEN);
        eb.addField("🧠 Memory",
                String.format("Used: %d MB\nTotal: %d MB\nMax: %d MB", usedMemory, totalMemory, maxMemory), true);
        eb.addField("⏱️ Uptime", uptimeStr, true);
        eb.addField("🧵 Threads", String.valueOf(threadCount), true);
        eb.addField("⚙️ CPU Usage", String.format("Process: %.1f%%\nSystem: %.1f%%", processCpu, systemCpu), true);

        event.replyEmbeds(eb.build()).queue();
    }
}
