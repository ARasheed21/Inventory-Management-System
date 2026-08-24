package com.example.inventory.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void locksAfterMaxFailuresAndReportsLocked() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, 60);

        assertThat(limiter.isLocked("user1")).isFalse();
        limiter.recordFailure("user1");
        limiter.recordFailure("user1");
        assertThat(limiter.isLocked("user1")).isFalse();
        limiter.recordFailure("user1");
        assertThat(limiter.isLocked("user1")).isTrue();
    }

    @Test
    void independentUsersDoNotAffectEachOther() {
        LoginRateLimiter limiter = new LoginRateLimiter(2, 60);

        limiter.recordFailure("userA");
        limiter.recordFailure("userA");

        assertThat(limiter.isLocked("userA")).isTrue();
        assertThat(limiter.isLocked("userB")).isFalse();
    }
}
