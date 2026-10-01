package com.cinekeep.auth;

import com.cinekeep.user.User;
import com.cinekeep.user.UserRepository;
import com.cinekeep.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final String unknownUserPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            RateLimiter rateLimiter,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.unknownUserPasswordHash = passwordEncoder.encode("unknown-user-password");
    }

    @Transactional
    public AuthResult register(RegisterRequest request, String clientIp) {
        rateLimiter.consume(RateLimitRule.REGISTER_BY_IP, clientIp);

        String email = normalizeEmail(request.email());
        String username = request.username().strip();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyExistsException();
        }

        User user = userRepository.save(new User(email, username, passwordEncoder.encode(request.password())));
        return issueTokens(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        rateLimiter.consume(RateLimitRule.LOGIN_BY_IP, clientIp);
        rateLimiter.ensureAvailable(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, email);

        Optional<User> user = userRepository.findByEmail(email);
        String passwordHash = user.map(User::getPasswordHash).orElse(unknownUserPasswordHash);

        if (!passwordEncoder.matches(request.password(), passwordHash) || user.isEmpty()) {
            rateLimiter.recordAttempt(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, email);
            throw new InvalidCredentialsException();
        }

        rateLimiter.reset(RateLimitRule.LOGIN_FAILURES_BY_EMAIL, email);
        return issueTokens(user.get());
    }

    public AuthResult refresh(String refreshToken, String clientIp) {
        rateLimiter.consume(RateLimitRule.REFRESH_BY_IP, clientIp);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RotatedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(refreshToken);
        return new AuthResult(toResponse(rotatedRefreshToken.user()), rotatedRefreshToken.refreshToken());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    private AuthResult issueTokens(User user) {
        return new AuthResult(toResponse(user), refreshTokenService.create(user));
    }

    private AuthResponse toResponse(User user) {
        IssuedAccessToken accessToken = jwtService.createAccessToken(user);
        long expiresIn = Duration.between(clock.instant(), accessToken.expiresAt()).toSeconds();

        return new AuthResponse(accessToken.value(), TOKEN_TYPE, expiresIn, UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
