package com.cinekeep.auth;

import com.cinekeep.common.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {
    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
