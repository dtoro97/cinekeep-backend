package com.cinekeep.auth;

import com.cinekeep.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
