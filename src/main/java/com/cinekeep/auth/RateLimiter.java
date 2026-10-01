package com.cinekeep.auth;

import com.cinekeep.common.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.TimeMeter;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {
    private static final Duration IDLE_BUCKET_EXPIRY = Duration.ofHours(2);
    private static final long MAXIMUM_BUCKETS = 100_000;

    private final Cache<String, Bucket> buckets;
    private final TimeMeter timeMeter;

    public RateLimiter(Clock clock) {
        this.timeMeter = new ClockTimeMeter(clock);
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(IDLE_BUCKET_EXPIRY)
                .maximumSize(MAXIMUM_BUCKETS)
                .build();
    }

    public void consume(RateLimitRule rule, String key) {
        ConsumptionProbe probe = bucket(rule, key).tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            throw tooManyRequests(rule, probe.getNanosToWaitForRefill());
        }
    }

    public void ensureAvailable(RateLimitRule rule, String key) {
        EstimationProbe probe = bucket(rule, key).estimateAbilityToConsume(1);

        if (!probe.canBeConsumed()) {
            throw tooManyRequests(rule, probe.getNanosToWaitForRefill());
        }
    }

    public void recordAttempt(RateLimitRule rule, String key) {
        bucket(rule, key).tryConsume(1);
    }

    public void reset(RateLimitRule rule, String key) {
        buckets.invalidate(cacheKey(rule, key));
    }

    private Bucket bucket(RateLimitRule rule, String key) {
        return buckets.get(cacheKey(rule, key), ignored -> Bucket.builder()
                .addLimit(limit -> limit.capacity(rule.getCapacity()).refillGreedy(rule.getCapacity(), rule.getPeriod()))
                .withCustomTimePrecision(timeMeter)
                .build());
    }

    private static String cacheKey(RateLimitRule rule, String key) {
        return rule.name() + ":" + key;
    }

    private static TooManyRequestsException tooManyRequests(RateLimitRule rule, long nanosToWait) {
        long retryAfterSeconds = Math.max(1, (long) Math.ceil(nanosToWait / (double) TimeUnit.SECONDS.toNanos(1)));
        return new TooManyRequestsException(rule.getMessage(), retryAfterSeconds);
    }

    private record ClockTimeMeter(Clock clock) implements TimeMeter {
        @Override
        public long currentTimeNanos() {
            return TimeUnit.MILLISECONDS.toNanos(clock.millis());
        }

        @Override
        public boolean isWallClockBased() {
            return true;
        }
    }
}
