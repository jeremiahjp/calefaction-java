package com.jp.calefaction.listeners.linkchecker;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public abstract class AbstractLinkChecker implements LinkChecker {

    private final String type;
    private final List<String> patterns;
    private final boolean disabled;

    protected AbstractLinkChecker(String type, List<String> patterns, boolean disabled) {
        this.type = type;
        this.patterns = patterns;
        this.disabled = disabled;
    }

    @Override
    public boolean matches(String message) {
        return patterns.stream().anyMatch(message::contains);
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public String getType() {
        return type;
    }
}
