package com.jp.calefaction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.time.Instant;
import lombok.Data;

@Entity
@Data
@IdClass(OriginalMessagesKey.class)
public class OriginalMessages {
    private String snowflakeId;
    private String originalUrl;

    @Id
    private String guildId;

    private String messageId;
    private String channelId;
    private Instant createdOn;
    private String capturedUrl;

    @Id
    private String urlKey;

    private String urlDomain;
}
