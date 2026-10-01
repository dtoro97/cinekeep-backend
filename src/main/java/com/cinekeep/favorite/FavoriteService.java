package com.cinekeep.favorite;

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
public class FavoriteService {
    private final CurrentUserService currentUserService;
    private final MediaItemService mediaItemService;
    private final FavoriteItemRepository favoriteItemRepository;

    public FavoriteService(
            CurrentUserService currentUserService,
            MediaItemService mediaItemService,
            FavoriteItemRepository favoriteItemRepository
    ) {
        this.currentUserService = currentUserService;
        this.mediaItemService = mediaItemService;
        this.favoriteItemRepository = favoriteItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<FavoriteItemResponse> getFavorites(
            MediaType mediaType,
            int page,
            int size,
            Sort.Direction sortDirection
    ) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "addedAt", "id"));

        Page<FavoriteItem> favoriteItems = mediaType == null
                ? favoriteItemRepository.findByUser_Id(userId, pageable)
                : favoriteItemRepository.findByUser_IdAndMediaItem_MediaType(userId, mediaType, pageable);

        return PageResponse.from(favoriteItems.map(FavoriteItemResponse::from));
    }

    @Transactional
    public FavoriteItemResponse addToFavorites(FavoriteItemRequest request) {
        User user = currentUserService.getCurrentUser();

        if (favoriteItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                user.getId(),
                request.mediaType(),
                request.tmdbId()
        )) {
            throw new FavoriteItemAlreadyExistsException();
        }

        MediaItem mediaItem = mediaItemService.findOrCreate(request.toMediaItemSnapshot());
        FavoriteItem favoriteItem = favoriteItemRepository.save(new FavoriteItem(user, mediaItem));

        return FavoriteItemResponse.from(favoriteItem);
    }

    @Transactional
    public void removeFromFavorites(MediaType mediaType, Integer tmdbId) {
        Long userId = currentUserService.getCurrentUser().getId();

        FavoriteItem favoriteItem = favoriteItemRepository
                .findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(userId, mediaType, tmdbId)
                .orElseThrow(FavoriteItemNotFoundException::new);

        favoriteItemRepository.delete(favoriteItem);
    }
}
