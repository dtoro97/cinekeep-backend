package com.cinekeep.auth;

import com.cinekeep.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties(
                new SecurityProperties.JwtProperties("secret", "cinekeep", Duration.ofMinutes(15)),
                new SecurityProperties.RefreshTokenProperties(Duration.ofDays(30), "cinekeep_refresh_token", false)
        );
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                securityProperties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        user = new User("david@example.com", "dtoro", "password-hash");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void createStoresOnlyHashOfRawToken() {
        String rawToken = refreshTokenService.create(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(RefreshTokenService.hash(rawToken))
                .isNotEqualTo(rawToken)
                .hasSize(64);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void createGeneratesDifferentTokens() {
        assertThat(refreshTokenService.create(user)).isNotEqualTo(refreshTokenService.create(user));
    }

    @Test
    void rotateRevokesCurrentTokenAndIssuesNewOne() {
        RefreshToken current = new RefreshToken(user, RefreshTokenService.hash("current"), NOW.plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("current"))).thenReturn(Optional.of(current));

        RotatedRefreshToken rotated = refreshTokenService.rotate("current");

        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(rotated.user()).isSameAs(user);
        assertThat(rotated.refreshToken()).isNotEqualTo("current");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotateWithRevokedTokenRevokesAllTokensOfUser() {
        RefreshToken reused = new RefreshToken(user, RefreshTokenService.hash("reused"), NOW.plusSeconds(60));
        reused.revoke(NOW.minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("reused"))).thenReturn(Optional.of(reused));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllActiveForUser(1L, NOW);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRejectsExpiredToken() {
        RefreshToken expired = new RefreshToken(user, RefreshTokenService.hash("expired"), NOW);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("expired"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeMarksActiveTokenAsRevoked() {
        RefreshToken active = new RefreshToken(user, RefreshTokenService.hash("active"), NOW.plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("active"))).thenReturn(Optional.of(active));

        refreshTokenService.revoke("active");

        assertThat(active.isRevoked()).isTrue();
    }
}
