package com.cinekeep.list;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserListRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull Boolean isPublic,
        @NotNull ListSortBy sortBy,
        @Valid ListCoverRequest cover
) {
}
