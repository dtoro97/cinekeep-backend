package com.cinekeep.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("cinekeep.security")
public record SecurityProperties(JwtProperties jwt, RefreshTokenProperties refreshToken) {
    public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {
    }

    public record RefreshTokenProperties(Duration ttl, String cookieName, boolean cookieSecure) {
    }
}
