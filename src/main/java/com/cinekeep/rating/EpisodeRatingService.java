package com.cinekeep.rating;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaItemService;
import com.cinekeep.media.MediaType;
import com.cinekeep.user.CurrentUserService;
import com.cinekeep.user.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EpisodeRatingService {
    private final CurrentUserService currentUserService;
    private final MediaItemService mediaItemService;
    private final EpisodeRatingRepository episodeRatingRepository;

    public EpisodeRatingService(
            CurrentUserService currentUserService,
            MediaItemService mediaItemService,
            EpisodeRatingRepository episodeRatingRepository
    ) {
        this.currentUserService = currentUserService;
        this.mediaItemService = mediaItemService;
        this.episodeRatingRepository = episodeRatingRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EpisodeRatingResponse> getEpisodeRatings(int page, int size, Sort.Direction sortDirection) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "ratedAt", "id"));

        return PageResponse.from(episodeRatingRepository.findByUser_Id(userId, pageable)
                .map(EpisodeRatingResponse::from));
    }

    @Transactional(readOnly = true)
    public EpisodeRatingResponse getEpisodeRating(Integer seriesTmdbId, Integer seasonNumber, Integer episodeNumber) {
        Long userId = currentUserService.getCurrentUser().getId();

        return findEpisodeRating(userId, seriesTmdbId, seasonNumber, episodeNumber)
                .map(EpisodeRatingResponse::from)
                .orElseThrow(EpisodeRatingNotFoundException::new);
    }

    @Transactional
    public EpisodeRatingResponse rateEpisode(
            Integer seriesTmdbId,
            Integer seasonNumber,
            Integer episodeNumber,
            EpisodeRatingRequest request
    ) {
        User user = currentUserService.getCurrentUser();
        MediaItem seriesItem = mediaItemService.findOrCreate(request.series().toMediaItemSnapshot(seriesTmdbId));

        EpisodeRating episodeRating = findEpisodeRating(user.getId(), seriesTmdbId, seasonNumber, episodeNumber)
                .map(existingRating -> {
                    existingRating.update(
                            request.episodeName(),
                            request.stillPath(),
                            request.airDate(),
                            request.value()
                    );
                    return existingRating;
                })
                .orElseGet(() -> new EpisodeRating(
                        user,
                        seriesItem,
                        seasonNumber,
                        episodeNumber,
                        request.episodeName(),
                        request.stillPath(),
                        request.airDate(),
                        request.value()
                ));

        return EpisodeRatingResponse.from(episodeRatingRepository.saveAndFlush(episodeRating));
    }

    @Transactional
    public void deleteEpisodeRating(Integer seriesTmdbId, Integer seasonNumber, Integer episodeNumber) {
        Long userId = currentUserService.getCurrentUser().getId();

        EpisodeRating episodeRating = findEpisodeRating(userId, seriesTmdbId, seasonNumber, episodeNumber)
                .orElseThrow(EpisodeRatingNotFoundException::new);

        episodeRatingRepository.delete(episodeRating);
    }

    private Optional<EpisodeRating> findEpisodeRating(
            Long userId,
            Integer seriesTmdbId,
            Integer seasonNumber,
            Integer episodeNumber
    ) {
        return episodeRatingRepository
                .findByUser_IdAndSeriesItem_MediaTypeAndSeriesItem_TmdbIdAndSeasonNumberAndEpisodeNumber(
                        userId,
                        MediaType.TV,
                        seriesTmdbId,
                        seasonNumber,
                        episodeNumber
                );
    }
}
