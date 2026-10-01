package com.cinekeep.watchlist;

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
class WatchlistServiceTest {
    private static final long USER_ID = 1L;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MediaItemService mediaItemService;

    @Mock
    private WatchlistItemRepository watchlistItemRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("dev@cinekeep.test", "dev", "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getWatchlistWithoutMediaTypeReturnsAllItemsSortedByAddedAt() {
        PageRequest expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "addedAt", "id"));
        Page<WatchlistItem> page = new PageImpl<>(List.of(watchlistItem(MediaType.MOVIE)), expectedPageable, 1);
        when(watchlistItemRepository.findByUser_Id(USER_ID, expectedPageable)).thenReturn(page);

        var response = watchlistService.getWatchlist(null, 0, 20, Sort.Direction.DESC);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getWatchlistWithMediaTypeFiltersByMediaType() {
        PageRequest expectedPageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "addedAt", "id"));
        when(watchlistItemRepository.findByUser_IdAndMediaItem_MediaType(USER_ID, MediaType.TV, expectedPageable))
                .thenReturn(Page.empty(expectedPageable));

        var response = watchlistService.getWatchlist(MediaType.TV, 1, 10, Sort.Direction.ASC);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        verify(watchlistItemRepository, never()).findByUser_Id(any(), any());
    }

    @Test
    void addToWatchlistSavesNewItem() {
        MediaItem mediaItem = mediaItem(MediaType.MOVIE);
        when(watchlistItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(false);
        when(mediaItemService.findOrCreate(any())).thenReturn(mediaItem);
        when(watchlistItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = watchlistService.addToWatchlist(request());

        assertThat(response.tmdbId()).isEqualTo(977942);
        assertThat(response.title()).isEqualTo("The Uprising");
    }

    @Test
    void addToWatchlistRejectsDuplicate() {
        when(watchlistItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(true);

        assertThatThrownBy(() -> watchlistService.addToWatchlist(request()))
                .isInstanceOf(WatchlistItemAlreadyExistsException.class);

        verify(watchlistItemRepository, never()).save(any());
    }

    @Test
    void removeFromWatchlistDeletesExistingItem() {
        WatchlistItem watchlistItem = watchlistItem(MediaType.MOVIE);
        when(watchlistItemRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(Optional.of(watchlistItem));

        watchlistService.removeFromWatchlist(MediaType.MOVIE, 977942);

        verify(watchlistItemRepository).delete(watchlistItem);
    }

    @Test
    void removeFromWatchlistRejectsMissingItem() {
        when(watchlistItemRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 977942)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.removeFromWatchlist(MediaType.MOVIE, 977942))
                .isInstanceOf(WatchlistItemNotFoundException.class);
    }

    private WatchlistItem watchlistItem(MediaType mediaType) {
        return new WatchlistItem(user, mediaItem(mediaType));
    }

    private static MediaItem mediaItem(MediaType mediaType) {
        return new MediaItem(977942, mediaType, "The Uprising", null, null, null, null, 7.5, 32);
    }

    private static WatchlistItemRequest request() {
        return new WatchlistItemRequest(977942, MediaType.MOVIE, "The Uprising", null, null, null, null, 7.5, 32);
    }
}
