package com.cinekeep.favorite;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaType;

import java.time.Instant;
import java.time.LocalDate;

public record FavoriteItemResponse(
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
    public static FavoriteItemResponse from(FavoriteItem favoriteItem) {
        MediaItem mediaItem = favoriteItem.getMediaItem();

        return new FavoriteItemResponse(
                favoriteItem.getId(),
                mediaItem.getTmdbId(),
                mediaItem.getMediaType(),
                mediaItem.getTitle(),
                mediaItem.getPosterPath(),
                mediaItem.getBackdropPath(),
                mediaItem.getOverview(),
                mediaItem.getReleaseDate(),
                mediaItem.getVoteAverage(),
                mediaItem.getVoteCount(),
                favoriteItem.getAddedAt()
        );
    }
}
