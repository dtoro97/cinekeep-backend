package com.cinekeep.watchlist;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaType;

import java.time.Instant;
import java.time.LocalDate;

public record WatchlistItemResponse(
        Long id,
        Integer tmdbId,
        MediaType mediaType,
        String title,
        String posterPath,
        String backdropPath,
        String overview,
        LocalDate releaseDate,
        Double voteAverage,
        Integer voteCount,
        Instant addedAt
) {
    public static WatchlistItemResponse from(WatchlistItem watchlistItem) {
        MediaItem mediaItem = watchlistItem.getMediaItem();

        return new WatchlistItemResponse(
                watchlistItem.getId(),
                mediaItem.getTmdbId(),
                mediaItem.getMediaType(),
                mediaItem.getTitle(),
                mediaItem.getPosterPath(),
                mediaItem.getBackdropPath(),
                mediaItem.getOverview(),
                mediaItem.getReleaseDate(),
                mediaItem.getVoteAverage(),
                mediaItem.getVoteCount(),
                watchlistItem.getAddedAt()
        );
    }
}
