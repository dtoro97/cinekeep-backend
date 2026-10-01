package com.cinekeep.list;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaType;

import java.time.Instant;
import java.time.LocalDate;

public record UserListItemResponse(
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
        String comment,
        Instant addedAt
) {
    public static UserListItemResponse from(UserListItem userListItem) {
        MediaItem mediaItem = userListItem.getMediaItem();

        return new UserListItemResponse(
                userListItem.getId(),
                mediaItem.getTmdbId(),
                mediaItem.getMediaType(),
                mediaItem.getTitle(),
                mediaItem.getPosterPath(),
                mediaItem.getBackdropPath(),
                mediaItem.getOverview(),
                mediaItem.getReleaseDate(),
                mediaItem.getVoteAverage(),
                mediaItem.getVoteCount(),
                userListItem.getComment(),
                userListItem.getAddedAt()
        );
    }
}
