package com.example.inventory.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${security.login.max-attempts:5}") int maxAttempts,
            @Value("${security.login.window-seconds:60}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean isLocked(String username) {
        Entry entry = entries.get(username);
        if (entry == null || entry.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.lockedUntil)) {
            entries.remove(username);
            return false;
        }
        return true;
    }

    public synchronized void recordFailure(String username) {
        Instant now = Instant.now();
        boolean[] created = {false};
        Entry entry = entries.compute(username, (key, existing) -> {
            if (existing == null || now.isAfter(existing.windowEnd)) {
                created[0] = true;
                return new Entry(now.plus(window));
            }
            return existing;
        });
        if (!created[0]) {
            entry.count++;
        }
        if (entry.count >= maxAttempts) {
            entry.lockedUntil = now.plus(window);
        }
    }

    private static final class Entry {
        private int count = 1;
        private volatile Instant windowEnd;
        private volatile Instant lockedUntil;

        private Entry(Instant windowEnd) {
            this.windowEnd = windowEnd;
        }
    }
}
