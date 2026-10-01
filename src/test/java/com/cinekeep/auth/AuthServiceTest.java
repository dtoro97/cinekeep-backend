package com.cinekeep.auth;

import com.cinekeep.common.TooManyRequestsException;
import com.cinekeep.user.User;
import com.cinekeep.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final String CLIENT_IP = "203.0.113.7";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RateLimiter rateLimiter;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("unknown-user-password")).thenReturn("unknown-user-hash");
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                rateLimiter,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndIssuesTokens() {
        when(userRepository.existsByEmail("david@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("dtoro")).thenReturn(false);
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed-password");
        when(userRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        stubTokens();

        AuthResult result = authService.register(
                new RegisterRequest(" David@Example.COM ", "dtoro", "correct horse"),
                CLIENT_IP
        );

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("david@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().expiresIn()).isEqualTo(900);
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(rateLimiter).consume(RateLimitRule.REGISTER_BY_IP, CLIENT_IP);
    }

    @Test
    void registerRejectsTakenEmail() {
        when(userRepository.existsByEmail("david@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("david@example.com", "dtoro", "correct horse"),
                CLIENT_IP
        )).isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsTakenUsername() {
        when(userRepository.existsByEmail("david@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("DToro")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("david@example.com", "DToro", "correct horse"),
                CLIENT_IP
        )).isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void registerStopsWhenIpIsRateLimited() {
        doThrow(new TooManyRequestsException("Too many registration attempts. Try again later.", 60))
                .when(rateLimiter).consume(RateLimitRule.REGISTER_BY_IP, CLIENT_IP);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("david@example.com", "dtoro", "correct horse"),
                CLIENT_IP
        )).isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void loginIssuesTokensAndResetsFailureCount() {
        User user = withId(new User("david@example.com", "dtoro", "stored-hash"));
        when(userRepository.findByEmail("david@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct horse", "stored-hash")).thenReturn(true);
        stubTokens();

        AuthResult result = authService.login(new LoginRequest("DAVID@example.com", "correct horse"), CLIENT_IP);

        assertThat(result.response().user().username()).isEqualTo("dtoro");
        verify(rateLimiter).consume(RateLimitRule.LOGIN_BY_IP, CLIENT_IP);
        verify(rateLimiter).ensureAvailable(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");
        verify(rateLimiter).reset(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");
        verify(rateLimiter, never()).recordAttempt(any(), any());
    }

    @Test
    void loginRejectsWrongPasswordAndRecordsFailure() {
        User user = withId(new User("david@example.com", "dtoro", "stored-hash"));
        when(userRepository.findByEmail("david@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong password", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("david@example.com", "wrong password"), CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(rateLimiter).recordAttempt(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void loginForUnknownEmailStillChecksPasswordAndFails() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "any password"), CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches("any password", "unknown-user-hash");
        verify(rateLimiter).recordAttempt(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "ghost@example.com");
    }

    @Test
    void loginStopsBeforePasswordCheckWhenEmailIsThrottled() {
        doThrow(new TooManyRequestsException("Too many login attempts. Try again later.", 120))
                .when(rateLimiter).ensureAvailable(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, "david@example.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("david@example.com", "correct horse"), CLIENT_IP))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(userRepository);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void refreshRejectsMissingToken() {
        assertThatThrownBy(() -> authService.refresh(null, CLIENT_IP)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh(" ", CLIENT_IP)).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refreshReturnsNewTokens() {
        User user = withId(new User("david@example.com", "dtoro", "stored-hash"));
        when(refreshTokenService.rotate("old-refresh-token"))
                .thenReturn(new RotatedRefreshToken(user, "new-refresh-token"));
        when(jwtService.createAccessToken(user)).thenReturn(new IssuedAccessToken("access-token", NOW.plusSeconds(900)));

        AuthResult result = authService.refresh("old-refresh-token", CLIENT_IP);

        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.response().accessToken()).isEqualTo("access-token");
        verify(rateLimiter).consume(RateLimitRule.REFRESH_BY_IP, CLIENT_IP);
    }

    @Test
    void logoutWithoutTokenDoesNothing() {
        authService.logout(null);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logoutRevokesToken() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }

    private void stubTokens() {
        when(jwtService.createAccessToken(any())).thenReturn(new IssuedAccessToken("access-token", NOW.plusSeconds(900)));
        when(refreshTokenService.create(any())).thenReturn("refresh-token");
    }

    private static User withId(User user) {
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
