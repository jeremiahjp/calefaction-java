package com.jp.calefaction.service.repost;

import com.jp.calefaction.entity.OriginalMessages;
import com.jp.calefaction.repository.OriginalMessagesRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class OriginalMessagesService {

    private final OriginalMessagesRepository originalMessagesRepository;

    public Mono<OriginalMessages> save(OriginalMessages originalMessages) {
        log.info("Attempting to save into Original_Messages with the object:\n{}", originalMessages);
        return originalMessagesRepository.save(originalMessages);
    }

    public Mono<OriginalMessages> findByUrlKeyAndGuildId(String urlKey, String guildId) {
        log.info("Finding original message by URL key {} and guild ID {}", urlKey, guildId);
        return originalMessagesRepository.findByUrlKeyAndGuildId(urlKey, guildId);
    }
}
