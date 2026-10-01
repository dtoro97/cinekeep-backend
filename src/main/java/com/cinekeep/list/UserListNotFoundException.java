package com.cinekeep.list;

import com.cinekeep.common.NotFoundException;

public class UserListNotFoundException extends NotFoundException {
    public UserListNotFoundException() {
        super("List not found");
    }
}
