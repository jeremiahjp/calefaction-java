package com.jp.calefaction.repository;

import com.jp.calefaction.entity.OriginalMessages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OriginalMessagesRepository extends JpaRepository<OriginalMessages, String> {

    OriginalMessages findByUrlKeyAndGuildId(String urlKey, String guildId);
}
