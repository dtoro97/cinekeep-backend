package com.cinekeep.media;

import com.cinekeep.favorite.FavoriteItemRepository;
import com.cinekeep.rating.Rating;
import com.cinekeep.rating.RatingRepository;
import com.cinekeep.user.CurrentUserService;
import com.cinekeep.user.User;
import com.cinekeep.watchlist.WatchlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaStateServiceTest {
    private static final long USER_ID = 1L;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private WatchlistItemRepository watchlistItemRepository;

    @Mock
    private FavoriteItemRepository favoriteItemRepository;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private MediaStateService mediaStateService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("dev");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getMediaStateCombinesWatchlistFavoriteAndRating() {
        MediaItem mediaItem = new MediaItem(550, MediaType.MOVIE, "Fight Club", null, null, null, null, 8.4, 30000);
        when(watchlistItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 550)).thenReturn(true);
        when(favoriteItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.MOVIE, 550)).thenReturn(false);
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.MOVIE, 550))
                .thenReturn(Optional.of(new Rating(user, mediaItem, 8.5)));

        MediaStateResponse response = mediaStateService.getMediaState(MediaType.MOVIE, 550);

        assertThat(response).isEqualTo(new MediaStateResponse(true, false, 8.5));
    }

    @Test
    void getMediaStateReturnsNullRatingWhenNotRated() {
        when(watchlistItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.TV, 1396)).thenReturn(false);
        when(favoriteItemRepository.existsByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                USER_ID, MediaType.TV, 1396)).thenReturn(true);
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.TV, 1396))
                .thenReturn(Optional.empty());

        MediaStateResponse response = mediaStateService.getMediaState(MediaType.TV, 1396);

        assertThat(response).isEqualTo(new MediaStateResponse(false, true, null));
    }
}
