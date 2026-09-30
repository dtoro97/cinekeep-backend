package com.cinekeep.media;

import java.time.LocalDate;

public record MediaItemSnapshot(
        Integer tmdbId,
        MediaType mediaType,
        String title,
        String posterPath,
        String backdropPath,
        String overview,
        LocalDate releaseDate,
        Double voteAverage,
        Integer voteCount
) {
}
