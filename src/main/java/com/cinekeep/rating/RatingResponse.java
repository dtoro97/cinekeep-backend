package com.cinekeep.rating;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaType;

import java.time.Instant;
import java.time.LocalDate;

public record RatingResponse(
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
        Double value,
        Instant ratedAt,
        Instant updatedAt
) {
    public static RatingResponse from(Rating rating) {
        MediaItem mediaItem = rating.getMediaItem();

        return new RatingResponse(
                rating.getId(),
                mediaItem.getTmdbId(),
                mediaItem.getMediaType(),
                mediaItem.getTitle(),
                mediaItem.getPosterPath(),
                mediaItem.getBackdropPath(),
                mediaItem.getOverview(),
                mediaItem.getReleaseDate(),
                mediaItem.getVoteAverage(),
                mediaItem.getVoteCount(),
                rating.getValue(),
                rating.getRatedAt(),
                rating.getUpdatedAt()
        );
    }
}
