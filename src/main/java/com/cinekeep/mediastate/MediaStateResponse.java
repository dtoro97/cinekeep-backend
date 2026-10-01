package com.cinekeep.mediastate;

public record MediaStateResponse(boolean inWatchlist, boolean favorite, Double rating) {
}
