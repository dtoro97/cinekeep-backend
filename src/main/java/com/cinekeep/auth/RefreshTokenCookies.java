package com.cinekeep.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Component
public class RefreshTokenCookies {
    private static final String COOKIE_PATH = "/api/auth";

    private final SecurityProperties securityProperties;

    public RefreshTokenCookies(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, securityProperties.refreshToken().ttl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, securityProperties.refreshToken().cookieName());
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(securityProperties.refreshToken().cookieName(), value)
                .httpOnly(true)
                .secure(securityProperties.refreshToken().cookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
