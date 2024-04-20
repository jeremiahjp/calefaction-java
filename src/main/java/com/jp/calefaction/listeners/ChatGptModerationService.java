package com.jp.calefaction.listeners;

import com.jp.calefaction.model.ai.ModerationResponse.Categories;
import com.jp.calefaction.service.ai.ChatGPTService;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class ChatGptModerationService {

    private final ChatGPTService chatGPTService;

    public Mono<List<String>> process(String message) {
        log.info("Processing the string for moderation");
        return chatGPTService.getChatModeration(message).flatMap(response -> {
            if (!response.getResults().isEmpty() && response.getResults().get(0).isFlagged()) {
                return Mono.just(
                        getFlaggedCategories(response.getResults().get(0).getCategories()));
            }
            return Mono.just(new ArrayList<String>());
        });
    }

    public List<String> getFlaggedCategories(Categories categories) {
        List<String> flaggedCategories = new ArrayList<>();

        if (categories.isSexual()) {
            flaggedCategories.add("sexual");
        }
        if (categories.isHate()) {
            flaggedCategories.add("hate");
        }
        if (categories.isHarassment()) {
            flaggedCategories.add("harassment");
        }
        if (categories.isSelfHarm()) {
            flaggedCategories.add("self-harm");
        }
        if (categories.isSexualMinors()) {
            flaggedCategories.add("sexual/minors");
        }
        if (categories.isHateThreatening()) {
            flaggedCategories.add("hate/threatening");
        }
        if (categories.isViolenceGraphic()) {
            flaggedCategories.add("violence/graphic");
        }
        if (categories.isSelfHarmIntent()) {
            flaggedCategories.add("self-harm/intent");
        }
        if (categories.isSelfHarmInstructions()) {
            flaggedCategories.add("self-harm/instructions");
        }
        if (categories.isHarassmentThreatening()) {
            flaggedCategories.add("harassment/threatening");
        }
        if (categories.isViolence()) {
            flaggedCategories.add("violence");
        }

        return flaggedCategories;
    }
}
