package com.cinekeep.list;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaType;

public record ListCoverResponse(
        Integer tmdbId,
        MediaType mediaType,
        String posterPath,
        String backdropPath,
        boolean selected
) {
    public static ListCoverResponse from(MediaItem mediaItem, boolean selected) {
        return new ListCoverResponse(
                mediaItem.getTmdbId(),
                mediaItem.getMediaType(),
                mediaItem.getPosterPath(),
                mediaItem.getBackdropPath(),
                selected
        );
    }
}
