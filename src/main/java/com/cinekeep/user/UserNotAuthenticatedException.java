package com.cinekeep.user;

import com.cinekeep.common.UnauthorizedException;

public class UserNotAuthenticatedException extends UnauthorizedException {
    public UserNotAuthenticatedException() {
        super("Authentication required");
    }
}
