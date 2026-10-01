package com.cinekeep.rating;

import com.cinekeep.media.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpisodeRatingRepository extends JpaRepository<EpisodeRating, Long> {
    @EntityGraph(attributePaths = "seriesItem")
    Page<EpisodeRating> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "seriesItem")
    Optional<EpisodeRating> findByUser_IdAndSeriesItem_MediaTypeAndSeriesItem_TmdbIdAndSeasonNumberAndEpisodeNumber(
            Long userId,
            MediaType mediaType,
            Integer seriesTmdbId,
            Integer seasonNumber,
            Integer episodeNumber
    );
}
