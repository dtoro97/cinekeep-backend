package com.cinekeep.watchlist;

import com.cinekeep.common.NotFoundException;

public class WatchlistItemNotFoundException extends NotFoundException {
    public WatchlistItemNotFoundException() {
        super("Watchlist item not found");
    }
}
