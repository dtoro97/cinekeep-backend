package com.cinekeep.rating;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaItemService;
import com.cinekeep.media.MediaItemSnapshot;
import com.cinekeep.media.MediaType;
import com.cinekeep.user.CurrentUserService;
import com.cinekeep.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class RatingServiceTest {
    private static final long USER_ID = 1L;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MediaItemService mediaItemService;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private RatingService ratingService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("dev@cinekeep.test", "dev", "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getRatingsWithoutMediaTypeReturnsAllRatingsSortedByRatedAt() {
        PageRequest expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "ratedAt", "id"));
        Page<Rating> page = new PageImpl<>(List.of(new Rating(user, mediaItem(), 8.0)), expectedPageable, 1);
        when(ratingRepository.findByUser_Id(USER_ID, expectedPageable)).thenReturn(page);

        var response = ratingService.getRatings(null, 0, 20, Sort.Direction.DESC);

        assertThat(response.content()).singleElement().extracting(RatingResponse::value).isEqualTo(8.0);
    }

    @Test
    void getRatingsWithMediaTypeFiltersByMediaType() {
        PageRequest expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "ratedAt", "id"));
        when(ratingRepository.findByUser_IdAndMediaItem_MediaType(USER_ID, MediaType.TV, expectedPageable))
                .thenReturn(Page.empty(expectedPageable));

        var response = ratingService.getRatings(MediaType.TV, 0, 20, Sort.Direction.ASC);

        assertThat(response.content()).isEmpty();
        verify(ratingRepository, never()).findByUser_Id(any(), any());
    }

    @Test
    void getRatingRejectsMissingRating() {
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.MOVIE, 550))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.getRating(MediaType.MOVIE, 550))
                .isInstanceOf(RatingNotFoundException.class);
    }

    @Test
    void rateCreatesRatingWhenNotRatedYet() {
        when(mediaItemService.findOrCreate(any())).thenReturn(mediaItem());
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.MOVIE, 550))
                .thenReturn(Optional.empty());
        when(ratingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = ratingService.rate(MediaType.MOVIE, 550, request(8.0));

        assertThat(response.value()).isEqualTo(8.0);
        assertThat(response.tmdbId()).isEqualTo(550);
    }

    @Test
    void rateUsesPathIdentityForMediaSnapshot() {
        when(mediaItemService.findOrCreate(any())).thenReturn(mediaItem());
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.MOVIE, 550))
                .thenReturn(Optional.empty());
        when(ratingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ratingService.rate(MediaType.MOVIE, 550, request(8.0));

        ArgumentCaptor<MediaItemSnapshot> snapshot = ArgumentCaptor.forClass(MediaItemSnapshot.class);
        verify(mediaItemService).findOrCreate(snapshot.capture());
        assertThat(snapshot.getValue().tmdbId()).isEqualTo(550);
        assertThat(snapshot.getValue().mediaType()).isEqualTo(MediaType.MOVIE);
    }

    @Test
    void rateUpdatesExistingRating() {
        Rating existingRating = new Rating(user, mediaItem(), 5.0);
        when(mediaItemService.findOrCreate(any())).thenReturn(mediaItem());
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.MOVIE, 550))
                .thenReturn(Optional.of(existingRating));
        when(ratingRepository.saveAndFlush(existingRating)).thenReturn(existingRating);

        var response = ratingService.rate(MediaType.MOVIE, 550, request(9.5));

        assertThat(response.value()).isEqualTo(9.5);
        assertThat(existingRating.getValue()).isEqualTo(9.5);
    }

    @Test
    void deleteRatingDeletesExistingRating() {
        Rating rating = new Rating(user, mediaItem(), 8.0);
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.MOVIE, 550))
                .thenReturn(Optional.of(rating));

        ratingService.deleteRating(MediaType.MOVIE, 550);

        verify(ratingRepository).delete(rating);
    }

    @Test
    void deleteRatingRejectsMissingRating() {
        when(ratingRepository.findByUser_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(USER_ID, MediaType.MOVIE, 550))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.deleteRating(MediaType.MOVIE, 550))
                .isInstanceOf(RatingNotFoundException.class);
    }

    private static MediaItem mediaItem() {
        return new MediaItem(550, MediaType.MOVIE, "Fight Club", null, null, null, null, 8.4, 30000);
    }

    private static RatingRequest request(double value) {
        return new RatingRequest(value, "Fight Club", null, null, null, null, 8.4, 30000);
    }
}
