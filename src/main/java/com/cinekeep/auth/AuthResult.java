package com.cinekeep.auth;

public record AuthResult(AuthResponse response, String refreshToken) {
}
