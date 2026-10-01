package com.cinekeep.list;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserListRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        Boolean isPublic,
        ListSortBy sortBy
) {
}
