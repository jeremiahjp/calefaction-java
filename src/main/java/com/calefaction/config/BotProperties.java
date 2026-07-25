package com.calefaction.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    private List<String> adminUserIds = new ArrayList<>();

    public List<String> getAdminUserIds() {
        return adminUserIds;
    }

    public void setAdminUserIds(List<String> adminUserIds) {
        this.adminUserIds = adminUserIds;
    }

    public boolean isAdmin(String userId) {
        return adminUserIds.contains(userId);
    }
}
