package com.cinekeep.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {
    Optional<MediaItem> findByMediaTypeAndTmdbId(MediaType mediaType, Integer tmdbId);
}
