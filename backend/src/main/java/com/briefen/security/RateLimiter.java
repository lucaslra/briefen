package com.briefen.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A minimal in-memory fixed-window rate limiter, keyed by an arbitrary string
 * (typically {@code clientIp:bucket}). No external dependencies.
 *
 * <p>Windows are evicted lazily: a stale window is reset on the next access, and
 * the whole map is swept when it grows past a bound, so memory stays bounded on a
 * self-hosted single instance without a scheduler.
 */
@Component
public class RateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Window(long windowStartMillis, AtomicInteger count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Records one hit for {@code key} and reports whether it is within the limit.
     *
     * @return true if allowed, false if the window's request budget is exhausted
     */
    public boolean tryAcquire(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_KEYS) {
            sweep(now, windowMillis);
        }
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMillis() >= windowMillis) {
                return new Window(now, new AtomicInteger(0));
            }
            return existing;
        });
        return window.count().incrementAndGet() <= maxRequests;
    }

    private void sweep(long now, long windowMillis) {
        windows.entrySet().removeIf(e -> now - e.getValue().windowStartMillis() >= windowMillis);
    }

    /** Test hook — clears all tracked windows. */
    void reset() {
        windows.clear();
    }
}
