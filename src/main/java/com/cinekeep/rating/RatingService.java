package com.cinekeep.rating;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaItemService;
import com.cinekeep.media.MediaType;
import com.cinekeep.user.CurrentUserService;
import com.cinekeep.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {
    private final CurrentUserService currentUserService;
    private final MediaItemService mediaItemService;
    private final RatingRepository ratingRepository;

    public RatingService(
            CurrentUserService currentUserService,
            MediaItemService mediaItemService,
            RatingRepository ratingRepository
    ) {
        this.currentUserService = currentUserService;
        this.mediaItemService = mediaItemService;
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RatingResponse> getRatings(
            MediaType mediaType,
            int page,
            int size,
            Sort.Direction sortDirection
    ) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "ratedAt", "id"));

        Page<Rating> ratings = mediaType == null
                ? ratingRepository.findByUser_Id(userId, pageable)
                : ratingRepository.findByUser_IdAndMediaItem_MediaType(userId, mediaType, pageable);

        return PageResponse.from(ratings.map(RatingResponse::from));
    }

    @Transactional(readOnly = true)
    public RatingResponse getRating(MediaType mediaType, Integer tmdbId) {
        Long userId = currentUserService.getCurrentUser().getId();

        return ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(userId, mediaType, tmdbId)
                .map(RatingResponse::from)
                .orElseThrow(RatingNotFoundException::new);
    }

    @Transactional
    public RatingResponse rate(MediaType mediaType, Integer tmdbId, RatingRequest request) {
        User user = currentUserService.getCurrentUser();
        MediaItem mediaItem = mediaItemService.findOrCreate(request.toMediaItemSnapshot(mediaType, tmdbId));

        Rating rating = ratingRepository
                .findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(user.getId(), mediaType, tmdbId)
                .map(existingRating -> {
                    existingRating.updateValue(request.value());
                    return existingRating;
                })
                .orElseGet(() -> new Rating(user, mediaItem, request.value()));

        return RatingResponse.from(ratingRepository.saveAndFlush(rating));
    }

    @Transactional
    public void deleteRating(MediaType mediaType, Integer tmdbId) {
        Long userId = currentUserService.getCurrentUser().getId();

        Rating rating = ratingRepository
                .findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(userId, mediaType, tmdbId)
                .orElseThrow(RatingNotFoundException::new);

        ratingRepository.delete(rating);
    }
}
