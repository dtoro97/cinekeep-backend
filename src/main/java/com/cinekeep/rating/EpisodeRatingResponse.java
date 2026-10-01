package com.cinekeep.rating;

import com.cinekeep.media.MediaItem;

import java.time.Instant;
import java.time.LocalDate;

public record EpisodeRatingResponse(
        Long id,
        Integer seriesTmdbId,
        String seriesTitle,
        String seriesPosterPath,
        Integer seasonNumber,
        Integer episodeNumber,
        String episodeName,
        String stillPath,
        LocalDate airDate,
        Double value,
        Instant ratedAt,
        Instant updatedAt
) {
    public static EpisodeRatingResponse from(EpisodeRating episodeRating) {
        MediaItem seriesItem = episodeRating.getSeriesItem();

        return new EpisodeRatingResponse(
                episodeRating.getId(),
                seriesItem.getTmdbId(),
                seriesItem.getTitle(),
                seriesItem.getPosterPath(),
                episodeRating.getSeasonNumber(),
                episodeRating.getEpisodeNumber(),
                episodeRating.getEpisodeName(),
                episodeRating.getStillPath(),
                episodeRating.getAirDate(),
                episodeRating.getValue(),
                episodeRating.getRatedAt(),
                episodeRating.getUpdatedAt()
        );
    }
}
