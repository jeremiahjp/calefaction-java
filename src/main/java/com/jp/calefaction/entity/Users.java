package com.jp.calefaction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Data;

@Entity
@Data
public class Users {
    @Id
    private String snowflakeId;

    private Instant createdOn;
}
