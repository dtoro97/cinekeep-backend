package com.cinekeep.auth;

import com.cinekeep.common.ConflictException;

public class UsernameAlreadyExistsException extends ConflictException {
    public UsernameAlreadyExistsException() {
        super("Username is already taken");
    }
}
