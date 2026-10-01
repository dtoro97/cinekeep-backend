package com.cinekeep.list;

import com.cinekeep.common.NotFoundException;

public class ListItemNotFoundException extends NotFoundException {
    public ListItemNotFoundException() {
        super("List item not found");
    }
}
