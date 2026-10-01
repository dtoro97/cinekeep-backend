package com.cinekeep.auth;

import java.time.Duration;

public enum RateLimitRule {
    LOGIN_BY_IP(20, Duration.ofMinutes(5), "Too many login attempts. Try again later."),
    LOGIN_FAILURES_BY_EMAIL(5, Duration.ofMinutes(15), "Too many login attempts. Try again later."),
    REGISTER_BY_IP(5, Duration.ofHours(1), "Too many registration attempts. Try again later."),
    REFRESH_BY_IP(60, Duration.ofMinutes(1), "Too many refresh requests. Try again later.");

    private final long capacity;
    private final Duration period;
    private final String message;

    RateLimitRule(long capacity, Duration period, String message) {
        this.capacity = capacity;
        this.period = period;
        this.message = message;
    }

    public long getCapacity() {
        return capacity;
    }

    public Duration getPeriod() {
        return period;
    }

    public String getMessage() {
        return message;
    }
}
