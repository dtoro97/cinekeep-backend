package com.cinekeep.mediastate;

import com.cinekeep.favorite.FavoriteItemRepository;
import com.cinekeep.media.MediaType;
import com.cinekeep.rating.Rating;
import com.cinekeep.rating.RatingRepository;
import com.cinekeep.user.CurrentUserService;
import com.cinekeep.watchlist.WatchlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaStateService {
    private final CurrentUserService currentUserService;
    private final WatchlistItemRepository watchlistItemRepository;
    private final FavoriteItemRepository favoriteItemRepository;
    private final RatingRepository ratingRepository;

    public MediaStateService(
            CurrentUserService currentUserService,
            WatchlistItemRepository watchlistItemRepository,
            FavoriteItemRepository favoriteItemRepository,
            RatingRepository ratingRepository
    ) {
        this.currentUserService = currentUserService;
        this.watchlistItemRepository = watchlistItemRepository;
        this.favoriteItemRepository = favoriteItemRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public MediaStateResponse getMediaState(MediaType mediaType, Integer tmdbId) {
        Long userId = currentUserService.getCurrentUser().getId();

        boolean inWatchlist = watchlistItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                userId,
                mediaType,
                tmdbId
        );
        boolean favorite = favoriteItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                userId,
                mediaType,
                tmdbId
        );
        Double rating = ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                        userId,
                        mediaType,
                        tmdbId
                )
                .map(Rating::getValue)
                .orElse(null);

        return new MediaStateResponse(inWatchlist, favorite, rating);
    }
}
