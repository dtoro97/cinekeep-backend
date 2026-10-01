package com.cinekeep.rating;

import com.cinekeep.media.MediaItemSnapshot;
import com.cinekeep.media.MediaType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SeriesSnapshotRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String posterPath,
        @Size(max = 255) String backdropPath,
        @Size(max = 2000) String overview,
        LocalDate releaseDate,
        @DecimalMin("0.0") @DecimalMax("10.0") Double voteAverage,
        @PositiveOrZero Integer voteCount
) {
    public MediaItemSnapshot toMediaItemSnapshot(Integer seriesTmdbId) {
        return new MediaItemSnapshot(
                seriesTmdbId,
                MediaType.TV,
                title,
                posterPath,
                backdropPath,
                overview,
                releaseDate,
                voteAverage,
                voteCount
        );
    }
}
