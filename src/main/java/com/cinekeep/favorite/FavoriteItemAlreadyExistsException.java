package com.cinekeep.favorite;

import com.cinekeep.common.ConflictException;

public class FavoriteItemAlreadyExistsException extends ConflictException {
    public FavoriteItemAlreadyExistsException() {
        super("Media item is already in favorites");
    }
}
