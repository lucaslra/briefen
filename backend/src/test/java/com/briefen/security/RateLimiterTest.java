package com.briefen.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsUpToMaxThenBlocksWithinWindow() {
        var limiter = new RateLimiter();
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("ip:oidc", 3, 60_000)).isTrue();
        }
        assertThat(limiter.tryAcquire("ip:oidc", 3, 60_000)).isFalse();
    }

    @Test
    void keysAreIndependent() {
        var limiter = new RateLimiter();
        assertThat(limiter.tryAcquire("a", 1, 60_000)).isTrue();
        assertThat(limiter.tryAcquire("a", 1, 60_000)).isFalse();
        // Different key has its own budget.
        assertThat(limiter.tryAcquire("b", 1, 60_000)).isTrue();
    }

    @Test
    void windowResetsAfterItElapses() throws InterruptedException {
        var limiter = new RateLimiter();
        assertThat(limiter.tryAcquire("k", 1, 50)).isTrue();
        assertThat(limiter.tryAcquire("k", 1, 50)).isFalse();
        Thread.sleep(60);
        // New window → budget refreshed.
        assertThat(limiter.tryAcquire("k", 1, 50)).isTrue();
    }
}
