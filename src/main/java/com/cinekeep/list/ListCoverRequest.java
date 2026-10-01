package com.cinekeep.list;

import com.cinekeep.media.MediaType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ListCoverRequest(
        @NotNull MediaType mediaType,
        @NotNull @Positive Integer tmdbId
) {
}
