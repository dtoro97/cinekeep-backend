package com.cinekeep.list;

import com.cinekeep.media.MediaItem;

import java.time.Instant;

public record UserListResponse(
        Long id,
        String name,
        String description,
        boolean isPublic,
        ListSortBy sortBy,
        long itemCount,
        ListCoverResponse cover,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserListResponse from(UserList userList, long itemCount, MediaItem latestMediaItem) {
        return new UserListResponse(
                userList.getId(),
                userList.getName(),
                userList.getDescription(),
                userList.isPublic(),
                userList.getSortBy(),
                itemCount,
                toCover(userList, latestMediaItem),
                userList.getCreatedAt(),
                userList.getUpdatedAt()
        );
    }

    private static ListCoverResponse toCover(UserList userList, MediaItem latestMediaItem) {
        if (userList.getCoverMediaItem() != null) {
            return ListCoverResponse.from(userList.getCoverMediaItem(), true);
        }

        return latestMediaItem == null ? null : ListCoverResponse.from(latestMediaItem, false);
    }
}
