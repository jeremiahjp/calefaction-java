// package com.jp.calefaction.commands;

// import com.jp.calefaction.service.RepostEmbedService;
// import com.jp.calefaction.service.RepostService;
// import com.jp.calefaction.service.repost.RepostCountService;
// import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
// import discord4j.core.object.command.ApplicationCommandInteractionOption;
// import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
// import java.util.Optional;
// import lombok.AllArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;
// import reactor.core.publisher.Mono;

// @Component
// @AllArgsConstructor
// @Slf4j
// public class RepostCommand implements SlashCommand {

//     private final RepostService repostService;
//     private final RepostEmbedService repostEmbedService;
//     private final RepostCountService repostCountService;

//     @Override
//     public String getName() {
//         return "repost";
//     }

//     public Mono<Void> handle(ChatInputInteractionEvent event) {
//         String mainCommand = event.getCommandName();
//         log.info("recived command name: {}", mainCommand);

//         Optional<String> subCommand = event.getOption("location")
//             .flatMap(ApplicationCommandInteractionOption::getValue)
//             .map(ApplicationCommandInteractionOptionValue::asString);

//         if (subCommand.isPresent()) {
//             String command = subCommand.get();

//             if (command.equals("top")) {

//             }
//         }
//         // return event.getOptions().stream()
//         //         .findFirst() // Get the first option, which should be your subcommand
//         //         .flatMap(subcommand -> {
//         //             String subcommandName = subcommand.getName();
//         //             switch (subcommandName) {
//         //                 case "top":
//         //                     return Mono.empty();
//         //                 case "check":
//         //                     return Mono.empty();
//         //                 default:
//         //                     return Mono.empty(); // Or handle unknown subcommand
//         //             }
//         //             return Mono.empty()
//         //         })
//         //         .orElse(Mono.empty());
//     }

//     private Mono<Void> handleTopCommand(
//             ChatInputInteractionEvent event, ApplicationCommandInteractionOption subcommand) {
//         // Extracting the 'category' option value if present
//         Optional<String> category = event.getOption("category")
//                 .flatMap(ApplicationCommandInteractionOption::getValue)
//                 .map(ApplicationCommandInteractionOptionValue::asString);

//         category.ifPresent(cat -> {
//             // Handle the 'top' command with the provided category
//             log.info("Top command called with category: " + cat);
//         });
//         return Mono.empty();
//         // // Reply to the command or perform other actions
//         // return event.reply("Handling 'top' command"
//         //         + (category.map(cat -> " with category " + cat).orElse("")));
//     }

//     private Mono<Void> handleCheckCommand(
//             ChatInputInteractionEvent event, ApplicationCommandInteractionOption subcommand) {
//         // Extracting the 'url' option value
//         String url = event.getOption("url")
//                 .flatMap(ApplicationCommandInteractionOption::getValue)
//                 .map(ApplicationCommandInteractionOptionValue::asString)
//                 .orElseThrow(() -> new IllegalArgumentException("URL is required"));

//         // Handle the 'check' command with the provided URL
//         log.info("Check command called with URL: " + url);

//         return Mono.empty();
//         // Reply to the command or perform other actions
//         return event.reply("Checking URL: " + url);
//     }
// }
