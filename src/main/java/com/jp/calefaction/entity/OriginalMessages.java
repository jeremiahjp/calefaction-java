package com.jp.calefaction.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "original_messages", schema = "repost")
public class OriginalMessages {

    @Id
    private UUID id;

    private String snowflakeId;
    private String originalUrl;

    private String guildId;

    private String messageId;
    private String channelId;
    private Instant createdOn;
    private String capturedUrl;

    private String urlKey;

    private String urlDomain;
}
