package com.cinekeep.auth;

import com.cinekeep.common.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {
    public EmailAlreadyExistsException() {
        super("Email is already registered");
    }
}
