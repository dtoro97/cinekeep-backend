package com.cinekeep.favorite;

import com.cinekeep.common.NotFoundException;

public class FavoriteItemNotFoundException extends NotFoundException {
    public FavoriteItemNotFoundException() {
        super("Favorite item not found");
    }
}
