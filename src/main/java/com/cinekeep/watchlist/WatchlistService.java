package com.cinekeep.watchlist;

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
public class WatchlistService {
    private final CurrentUserService currentUserService;
    private final MediaItemService mediaItemService;
    private final WatchlistItemRepository watchlistItemRepository;

    public WatchlistService(
            CurrentUserService currentUserService,
            MediaItemService mediaItemService,
            WatchlistItemRepository watchlistItemRepository
    ) {
        this.currentUserService = currentUserService;
        this.mediaItemService = mediaItemService;
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<WatchlistItemResponse> getWatchlist(
            MediaType mediaType,
            int page,
            int size,
            Sort.Direction sortDirection
    ) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "addedAt", "id"));

        Page<WatchlistItem> watchlistItems = mediaType == null
                ? watchlistItemRepository.findByUser_Id(userId, pageable)
                : watchlistItemRepository.findByUser_IdAndMediaItem_MediaType(userId, mediaType, pageable);

        return PageResponse.from(watchlistItems.map(WatchlistItemResponse::from));
    }

    @Transactional
    public WatchlistItemResponse addToWatchlist(WatchlistItemRequest request) {
        User user = currentUserService.getCurrentUser();

        if (watchlistItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                user.getId(),
                request.mediaType(),
                request.tmdbId()
        )) {
            throw new WatchlistItemAlreadyExistsException();
        }

        MediaItem mediaItem = mediaItemService.findOrCreate(request.toMediaItemSnapshot());
        WatchlistItem watchlistItem = watchlistItemRepository.save(new WatchlistItem(user, mediaItem));

        return WatchlistItemResponse.from(watchlistItem);
    }

    @Transactional
    public void removeFromWatchlist(MediaType mediaType, Integer tmdbId) {
        Long userId = currentUserService.getCurrentUser().getId();

        WatchlistItem watchlistItem = watchlistItemRepository
                .findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(userId, mediaType, tmdbId)
                .orElseThrow(WatchlistItemNotFoundException::new);

        watchlistItemRepository.delete(watchlistItem);
    }
}
