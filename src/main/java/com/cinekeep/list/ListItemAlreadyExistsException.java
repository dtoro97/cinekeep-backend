package com.cinekeep.list;

import com.cinekeep.common.ConflictException;

public class ListItemAlreadyExistsException extends ConflictException {
    public ListItemAlreadyExistsException() {
        super("Media item is already in the list");
    }
}
