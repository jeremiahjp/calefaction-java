package com.calefaction.features.weather;

import com.calefaction.core.CommandRegistry;
import com.calefaction.core.SlashCommand;
import com.calefaction.features.weather.dto.Current;
import com.calefaction.features.weather.dto.Daily;
import com.calefaction.features.weather.dto.OpenWeatherOneCallResponse;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class WeatherCommand implements SlashCommand {

    private final CommandRegistry commandRegistry;
    private final GeocodingService geocodingService;
    private final OpenWeatherService openWeatherService;

    public WeatherCommand(CommandRegistry commandRegistry, GeocodingService geocodingService,
            OpenWeatherService openWeatherService) {
        this.commandRegistry = commandRegistry;
        this.geocodingService = geocodingService;
        this.openWeatherService = openWeatherService;
    }

    @PostConstruct
    public void init() {
        commandRegistry.register(this);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("weather", "Get current weather for a location")
                .addOptions(
                        new OptionData(OptionType.STRING, "location", "City, Zip, Address, etc.", true),
                        new OptionData(OptionType.STRING, "units", "Temperature units", true)
                                .addChoice("Celsius (°C)", "metric")
                                .addChoice("Fahrenheit (°F)", "imperial"));
    }

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String location = event.getOption("location").getAsString();
        String units = event.getOption("units").getAsString();

        event.deferReply().queue();

        geocodingService.resolve(location)
                .flatMap(geo -> openWeatherService.getOneCall(geo.latitude(), geo.longitude(), units)
                        .map(weather -> new WeatherContext(geo, weather)))
                .switchIfEmpty(Mono.defer(() -> {
                    event.getHook().sendMessage("Could not find location or weather data.").queue();
                    return Mono.empty();
                }))
                .subscribe(ctx -> {
                    GeocodingService.GeoLocation geo = ctx.geo();
                    OpenWeatherOneCallResponse weather = ctx.weather();
                    String view = "current";

                    EmbedBuilder eb = buildEmbed(view, weather, units, geo.name());

                    String idBase = "weather:%s:" + geo.latitude() + ":" + geo.longitude() + ":" + units + ":"
                            + geo.name().replaceAll(":", "");

                    event.getHook().sendMessageEmbeds(eb.build())
                            .setComponents(ActionRow.of(getButtons(idBase, view)))
                            .queue();
                },
                        error -> event.getHook().sendMessage("Error resolving weather: " + error.getMessage()).queue());
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (parts.length < 5)
            return;

        String view = parts[1];
        double lat = Double.parseDouble(parts[2]);
        double lon = Double.parseDouble(parts[3]);
        String units = parts[4];
        String locationName = parts.length > 5 ? parts[5] : "Unknown";

        event.deferEdit().queue();

        openWeatherService.getOneCall(lat, lon, units)
                .switchIfEmpty(Mono.defer(() -> {
                    event.getHook().sendMessage("Could not fetch weather data.").setEphemeral(true).queue();
                    return Mono.empty();
                }))
                .subscribe(weather -> {
                    EmbedBuilder eb = buildEmbed(view, weather, units, locationName);
                    String idBase = "weather:%s:" + lat + ":" + lon + ":" + units + ":" + locationName;

                    event.getHook().editOriginalEmbeds(eb.build())
                            .setComponents(ActionRow.of(getButtons(idBase, view)))
                            .queue();
                }, error -> event.getHook().sendMessage("Error fetching weather: " + error.getMessage())
                        .setEphemeral(true)
                        .queue());
    }

    private java.util.List<Button> getButtons(String idBase, String currentView) {
        return java.util.List.of(
                Button.primary(Objects.requireNonNull(String.format(idBase, "current")), "Current")
                        .withDisabled("current".equals(currentView)),
                Button.secondary(Objects.requireNonNull(String.format(idBase, "detailed")), "Detailed")
                        .withDisabled("detailed".equals(currentView)),
                Button.secondary(Objects.requireNonNull(String.format(idBase, "today")), "Today's Forecast")
                        .withDisabled("today".equals(currentView)),
                Button.secondary(Objects.requireNonNull(String.format(idBase, "3day")), "3 Days")
                        .withDisabled("3day".equals(currentView)),
                Button.secondary(Objects.requireNonNull(String.format(idBase, "5day")), "5 Days")
                        .withDisabled("5day".equals(currentView)));
    }

    private EmbedBuilder buildEmbed(String view, OpenWeatherOneCallResponse data, String units, String locationName) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Weather: " + locationName);
        eb.setColor(Color.CYAN);
        eb.setFooter("weather.com (OpenWeatherMap)");

        String unitSym = units.equals("imperial") ? "°F" : "°C";

        switch (view) {
            case "current" -> {
                Current c = data.current();
                eb.setDescription(String.format("**%s**", c.weather().get(0).description()));
                eb.addField("Temp", String.format("%.1f %s", c.temp(), unitSym), true);
                eb.addField("Feels Like", String.format("%.1f %s", c.feels_like(), unitSym), true);
                eb.addField("Humidity", c.humidity() + "%", true);
                eb.addField("Wind", c.wind_speed() + (units.equals("imperial") ? " mph" : " m/s"), true);
                eb.addField("UV Index", String.valueOf(c.uvi()), true);
            }
            case "detailed" -> {
                Current c = data.current();
                eb.setDescription(String.format("**%s** (Detailed)", c.weather().get(0).description()));
                eb.addField("Temp", String.format("%.1f %s", c.temp(), unitSym), true);
                eb.addField("Feels Like", String.format("%.1f %s", c.feels_like(), unitSym), true);
                eb.addField("Humidity", c.humidity() + "%", true);
                eb.addField("Dew Point", String.format("%.1f %s", c.dew_point(), unitSym), true);
                eb.addField("Pressure", c.pressure() + " hPa", true);
                eb.addField("Visibility", (c.visibility() / 1000.0) + " km", true);
                eb.addField("Clouds", c.clouds() + "%", true);
                eb.addField("Wind", c.wind_speed() + (units.equals("imperial") ? " mph" : " m/s"), true);
                eb.addField("UV Index", String.valueOf(c.uvi()), true);

                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of(data.timezone()));
                eb.addField("Sunrise", timeFmt.format(Instant.ofEpochSecond(c.sunrise())), true);
                eb.addField("Sunset", timeFmt.format(Instant.ofEpochSecond(c.sunset())), true);
            }
            case "today" -> {
                if (!data.daily().isEmpty()) {
                    Daily d = data.daily().get(0);
                    eb.setDescription("**Today's Forecast**\n"
                            + (d.summary() != null ? d.summary() : d.weather().get(0).description()));
                    eb.addField("High/Low", String.format("%.1f / %.1f %s", d.temp().max(), d.temp().min(), unitSym),
                            true);
                    eb.addField("Morning", String.format("%.1f %s", d.temp().morn(), unitSym), true);
                    eb.addField("Day", String.format("%.1f %s", d.temp().day(), unitSym), true);
                    eb.addField("Evening", String.format("%.1f %s", d.temp().eve(), unitSym), true);
                    eb.addField("Night", String.format("%.1f %s", d.temp().night(), unitSym), true);
                    eb.addField("Rain Chance", String.format("%.0f%%", d.pop() * 100), true);
                }
            }
            case "3day", "5day" -> {
                int days = view.equals("3day") ? 3 : 5;
                eb.setDescription(days + " Day Forecast");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, MMM d").withZone(ZoneId.of(data.timezone()));

                for (int i = 0; i < Math.min(days, data.daily().size()); i++) {
                    Daily d = data.daily().get(i);
                    String dateStr = dtf.format(Instant.ofEpochSecond(d.dt()));
                    String desc = d.weather().get(0).main();
                    String val = String.format("%s | %.0f / %.0f %s | ☔ %.0f%%", desc, d.temp().max(), d.temp().min(),
                            unitSym, d.pop() * 100);
                    eb.addField(dateStr, val, false);
                }
            }
        }
        return eb;
    }

    private record WeatherContext(GeocodingService.GeoLocation geo, OpenWeatherOneCallResponse weather) {
    }
}
