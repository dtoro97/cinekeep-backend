package com.cinekeep.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenCookies refreshTokenCookies;

    public AuthController(AuthService authService, RefreshTokenCookies refreshTokenCookies) {
        this.authService = authService;
        this.refreshTokenCookies = refreshTokenCookies;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        return withRefreshCookie(HttpStatus.CREATED, authService.register(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return withRefreshCookie(HttpStatus.OK, authService.login(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        return withRefreshCookie(
                HttpStatus.OK,
                authService.refresh(refreshTokenCookies.read(request), request.getRemoteAddr())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(refreshTokenCookies.read(request));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(HttpStatus status, AuthResult authResult) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.create(authResult.refreshToken()).toString())
                .body(authResult.response());
    }
}
