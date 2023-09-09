package com.jp.calefaction.entity;

import java.io.Serializable;
import lombok.Data;

@Data
public class OriginalMessagesKey implements Serializable {
    private String urlKey;
    private String guildId;
}
