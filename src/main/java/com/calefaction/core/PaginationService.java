package com.calefaction.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class PaginationService {

    private final Cache<String, List<String>> cache;

    public PaginationService() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

    public void store(String id, List<String> chunks) {
        cache.put(id, chunks);
    }

    public List<String> get(String id) {
        return cache.getIfPresent(id);
    }

    public List<String> splitMessage(String message, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = message.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(length, start + chunkSize);
            if (end < length) {
                // Try to split at the last newline within the chunk
                int lastNewline = message.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline + 1;
                } else {
                    // Fall back to the last space
                    int lastSpace = message.lastIndexOf(' ', end);
                    if (lastSpace > start) {
                        end = lastSpace + 1;
                    }
                }
            }
            chunks.add(message.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
