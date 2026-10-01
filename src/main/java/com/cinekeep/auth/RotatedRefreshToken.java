package com.cinekeep.auth;

import com.cinekeep.user.User;

public record RotatedRefreshToken(User user, String refreshToken) {
}
