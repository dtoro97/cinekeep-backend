package com.cinekeep.favorite;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaItemService;
import com.cinekeep.media.MediaType;
import com.cinekeep.user.CurrentUserService;
import com.cinekeep.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    private static final long USER_ID = 1L;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MediaItemService mediaItemService;

    @Mock
    private FavoriteItemRepository favoriteItemRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("dev@cinekeep.test", "dev", "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getFavoritesWithoutMediaTypeReturnsAllItemsSortedByAddedAt() {
        PageRequest expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "addedAt", "id"));
        Page<FavoriteItem> page = new PageImpl<>(List.of(favoriteItem(MediaType.MOVIE)), expectedPageable, 1);
        when(favoriteItemRepository.findByUser_Id(USER_ID, expectedPageable)).thenReturn(page);

        var response = favoriteService.getFavorites(null, 0, 20, Sort.Direction.DESC);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getFavoritesWithMediaTypeFiltersByMediaType() {
        PageRequest expectedPageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "addedAt", "id"));
        when(favoriteItemRepository.findByUser_IdAndMediaItem_MediaType(USER_ID, MediaType.TV, expectedPageable))
                .thenReturn(Page.empty(expectedPageable));

        var response = favoriteService.getFavorites(MediaType.TV, 1, 10, Sort.Direction.ASC);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        verify(favoriteItemRepository, never()).findByUser_Id(any(), any());
    }

    @Test
    void addToFavoritesSavesNewItem() {
        MediaItem mediaItem = mediaItem(MediaType.MOVIE);
        when(favoriteItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(false);
        when(mediaItemService.findOrCreate(any())).thenReturn(mediaItem);
        when(favoriteItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = favoriteService.addToFavorites(request());

        assertThat(response.tmdbId()).isEqualTo(977942);
        assertThat(response.title()).isEqualTo("The Uprising");
    }

    @Test
    void addToFavoritesRejectsDuplicate() {
        when(favoriteItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addToFavorites(request()))
                .isInstanceOf(FavoriteItemAlreadyExistsException.class);

        verify(favoriteItemRepository, never()).save(any());
    }

    @Test
    void removeFromFavoritesDeletesExistingItem() {
        FavoriteItem favoriteItem = favoriteItem(MediaType.MOVIE);
        when(favoriteItemRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(Optional.of(favoriteItem));

        favoriteService.removeFromFavorites(MediaType.MOVIE, 977942);

        verify(favoriteItemRepository).delete(favoriteItem);
    }

    @Test
    void removeFromFavoritesRejectsMissingItem() {
        when(favoriteItemRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFromFavorites(MediaType.MOVIE, 977942))
                .isInstanceOf(FavoriteItemNotFoundException.class);
    }

    private FavoriteItem favoriteItem(MediaType mediaType) {
        return new FavoriteItem(user, mediaItem(mediaType));
    }

    private static MediaItem mediaItem(MediaType mediaType) {
        return new MediaItem(977942, mediaType, "The Uprising", null, null, null, null, 7.5, 32);
    }

    private static FavoriteItemRequest request() {
        return new FavoriteItemRequest(977942, MediaType.MOVIE, "The Uprising", null, null, null, null, 7.5, 32);
    }
}
