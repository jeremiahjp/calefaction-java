package com.jp.calefaction.entity.repost;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "repost_count", schema = "repost")
@Data
public class RepostCount {

    @Id
    private UUID id;

    private String guildId;

    @Column("snowflake_id")
    private String snowflakeId;

    @Column("attempted_repost_url_key")
    private String attemptedRepostUrlKey;

    @Column("created_on")
    private Instant createdOn;

    @Column("last_attempted")
    private Instant lastAttempted;
}
