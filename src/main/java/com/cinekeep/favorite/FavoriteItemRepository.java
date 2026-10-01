package com.cinekeep.favorite;

import com.cinekeep.media.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    @EntityGraph(attributePaths = "mediaItem")
    Page<FavoriteItem> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "mediaItem")
    Page<FavoriteItem> findByUser_IdAndMediaItem_MediaType(Long userId, MediaType mediaType, Pageable pageable);

    Optional<FavoriteItem> findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
            Long userId,
            MediaType mediaType,
            Integer tmdbId
    );

    boolean existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
            Long userId,
            MediaType mediaType,
            Integer tmdbId
    );
}
