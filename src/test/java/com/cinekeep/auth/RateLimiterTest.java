package com.cinekeep.auth;

import com.cinekeep.common.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-01T10:00:00Z"));
    private final RateLimiter rateLimiter = new RateLimiter(clock);

    @Test
    void consumeAllowsRequestsUpToCapacity() {
        for (int attempt = 0; attempt < RateLimitRule.REGISTER_BY_IP.getCapacity(); attempt++) {
            rateLimiter.consume(RateLimitRule.REGISTER_BY_IP, "203.0.113.7");
        }

        assertThatThrownBy(() -> rateLimiter.consume(RateLimitRule.REGISTER_BY_IP, "203.0.113.7"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("Too many registration attempts. Try again later.");
                    assertThat(exception.getRetryAfterSeconds()).isEqualTo(720);
                });
    }

    @Test
    void consumeRefillsOverTime() {
        exhaust(RateLimitRule.REGISTER_BY_IP, "203.0.113.7");

        clock.advance(Duration.ofMinutes(12));

        assertThatCode(() -> rateLimiter.consume(RateLimitRule.REGISTER_BY_IP, "203.0.113.7"))
                .doesNotThrowAnyException();
    }

    @Test
    void keysAreLimitedIndependently() {
        exhaust(RateLimitRule.REGISTER_BY_IP, "203.0.113.7");

        assertThatCode(() -> rateLimiter.consume(RateLimitRule.REGISTER_BY_IP, "198.51.100.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void rulesAreLimitedIndependently() {
        exhaust(RateLimitRule.REGISTER_BY_IP, "203.0.113.7");

        assertThatCode(() -> rateLimiter.consume(RateLimitRule.LOGIN_BY_IP, "203.0.113.7"))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureAvailableDoesNotConsume() {
        for (int check = 0; check < 10; check++) {
            rateLimiter.ensureAvailable(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");
        }

        assertThatCode(() -> rateLimiter.consume(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void recordedFailuresThrottleEmailUntilReset() {
        for (int failure = 0; failure < RateLimitRule.LOGIN_FAILURES_BY_EMAIL.getCapacity(); failure++) {
            rateLimiter.recordAttempt(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");
        }

        assertThatThrownBy(() -> rateLimiter.ensureAvailable(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com"))
                .isInstanceOf(TooManyRequestsException.class);

        rateLimiter.reset(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");

        assertThatCode(() -> rateLimiter.ensureAvailable(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void recordAttemptNeverThrowsWhenBucketIsEmpty() {
        for (int failure = 0; failure < 20; failure++) {
            rateLimiter.recordAttempt(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");
        }

        assertThatThrownBy(() -> rateLimiter.ensureAvailable(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    private void exhaust(RateLimitRule rule, String key) {
        for (int attempt = 0; attempt < rule.getCapacity(); attempt++) {
            rateLimiter.consume(rule, key);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
