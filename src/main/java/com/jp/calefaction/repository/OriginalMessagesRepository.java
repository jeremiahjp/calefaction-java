package com.jp.calefaction.repository;

import com.jp.calefaction.entity.OriginalMessages;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OriginalMessagesRepository extends ReactiveCrudRepository<OriginalMessages, String> {

    // @Query("SELECT * FROM original_messages WHERE url_key = :urlKey AND guild_id = :guildId")
    // Mono<OriginalMessages> findByUrlKeyAndGuildId(String urlKey, String guildId);

    // Example custom query that selects specific columns, excluding the composite key
    // @Query("SELECT snowflake_id, original_url, guild_id, message_id, channel_id, created_on, captured_url, url_key,"
    //         + " url_domain FROM repost.original_messages WHERE url_key = :urlKey AND guild_id = :guildId")
    Mono<OriginalMessages> findByUrlKeyAndGuildId(String urlKey, String guildId);
}
