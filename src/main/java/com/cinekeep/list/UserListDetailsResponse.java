package com.cinekeep.list;

import com.cinekeep.common.PageResponse;

public record UserListDetailsResponse(
        UserListResponse list,
        ListSortBy appliedSortBy,
        PageResponse<UserListItemResponse> items
) {
}
