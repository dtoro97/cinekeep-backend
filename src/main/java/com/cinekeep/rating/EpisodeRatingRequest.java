package com.cinekeep.rating;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EpisodeRatingRequest(
        @NotNull @RatingValue Double value,
        @NotBlank @Size(max = 255) String episodeName,
        @Size(max = 255) String stillPath,
        LocalDate airDate,
        @NotNull @Valid SeriesSnapshotRequest series
) {
}
