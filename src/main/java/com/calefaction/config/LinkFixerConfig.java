package com.calefaction.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "link-fixer")
public class LinkFixerConfig {

    private boolean enabled;
    private List<DomainConfig> domains = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<DomainConfig> getDomains() {
        return domains;
    }

    public void setDomains(List<DomainConfig> domains) {
        this.domains = domains;
    }

    public static class DomainConfig {
        private String pattern;
        private String replacement;
        private boolean enabled = true;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
