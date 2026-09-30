package com.cinekeep.watchlist;

import com.cinekeep.common.ConflictException;

public class WatchlistItemAlreadyExistsException extends ConflictException {
    public WatchlistItemAlreadyExistsException() {
        super("Media item is already in the watchlist");
    }
}
