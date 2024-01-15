package com.jp.calefaction.entity;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "users", schema = "repost")
public class Users {
    @Id
    private String snowflakeId;

    private Instant createdOn;
}
