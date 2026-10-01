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
    private final Clock clock;
    private final String unknownUserPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
        this.unknownUserPasswordHash = passwordEncoder.encode("unknown-user-password");
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
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
    public AuthResult login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(request.email()));
        String passwordHash = user.map(User::getPasswordHash).orElse(unknownUserPasswordHash);

        if (!passwordEncoder.matches(request.password(), passwordHash) || user.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user.get());
    }

    public AuthResult refresh(String refreshToken) {
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
