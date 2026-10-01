package com.cinekeep.auth;

import com.cinekeep.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final Instant NOW = Instant.now();

    private final SecurityProperties securityProperties = securityProperties("test-secret-that-is-long-enough-for-hs256");
    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtDecoder jwtDecoder = securityConfig.jwtDecoder(securityProperties);
    private final JwtService jwtService = new JwtService(
            securityConfig.jwtEncoder(securityProperties),
            securityProperties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createAccessTokenProducesTokenThatDecoderAccepts() {
        IssuedAccessToken accessToken = jwtService.createAccessToken(user());

        Jwt jwt = jwtDecoder.decode(accessToken.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("cinekeep");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("dtoro");
        assertThat(accessToken.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void decoderRejectsTokenSignedWithDifferentSecret() {
        SecurityProperties otherProperties = securityProperties("another-secret-that-is-long-enough-for-hs256");
        JwtService otherJwtService = new JwtService(
                securityConfig.jwtEncoder(otherProperties),
                otherProperties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        String foreignToken = otherJwtService.createAccessToken(user()).value();

        assertThatThrownBy(() -> jwtDecoder.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    private static User user() {
        User user = new User("david@example.com", "dtoro", "password-hash");
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    private static SecurityProperties securityProperties(String secret) {
        return new SecurityProperties(
                new SecurityProperties.JwtProperties(secret, "cinekeep", Duration.ofMinutes(15)),
                new SecurityProperties.RefreshTokenProperties(Duration.ofDays(30), "cinekeep_refresh_token", false)
        );
    }
}
