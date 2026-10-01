package com.cinekeep.list;

import com.cinekeep.common.BadRequestException;

public class CoverItemNotInListException extends BadRequestException {
    public CoverItemNotInListException() {
        super("Cover must be an item in the list");
    }
}
