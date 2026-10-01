package com.cinekeep.rating;

import com.cinekeep.media.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    @EntityGraph(attributePaths = "mediaItem")
    Page<Rating> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "mediaItem")
    Page<Rating> findByUser_IdAndMediaItem_MediaType(Long userId, MediaType mediaType, Pageable pageable);

    @EntityGraph(attributePaths = "mediaItem")
    Optional<Rating> findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
            Long userId,
            MediaType mediaType,
            Integer tmdbId
    );
}
