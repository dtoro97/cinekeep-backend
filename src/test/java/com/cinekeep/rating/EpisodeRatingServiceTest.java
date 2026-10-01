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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpisodeRatingServiceTest {
    private static final long USER_ID = 1L;
    private static final int SERIES_TMDB_ID = 1396;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MediaItemService mediaItemService;

    @Mock
    private EpisodeRatingRepository episodeRatingRepository;

    @InjectMocks
    private EpisodeRatingService episodeRatingService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("dev@cinekeep.test", "dev", "password-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getEpisodeRatingsReturnsRatingsSortedByRatedAt() {
        PageRequest expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "ratedAt", "id"));
        when(episodeRatingRepository.findByUser_Id(USER_ID, expectedPageable))
                .thenReturn(new PageImpl<>(List.of(episodeRating(9.0)), expectedPageable, 1));

        var response = episodeRatingService.getEpisodeRatings(0, 20, Sort.Direction.DESC);

        assertThat(response.content()).singleElement().satisfies(rating -> {
            assertThat(rating.seriesTitle()).isEqualTo("Breaking Bad");
            assertThat(rating.value()).isEqualTo(9.0);
        });
    }

    @Test
    void getEpisodeRatingRejectsMissingRating() {
        whenFindingEpisodeRating().thenReturn(Optional.empty());

        assertThatThrownBy(() -> episodeRatingService.getEpisodeRating(SERIES_TMDB_ID, 1, 1))
                .isInstanceOf(EpisodeRatingNotFoundException.class);
    }

    @Test
    void rateEpisodeCreatesRatingForTvSeries() {
        when(mediaItemService.findOrCreate(any())).thenReturn(seriesItem());
        whenFindingEpisodeRating().thenReturn(Optional.empty());
        when(episodeRatingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = episodeRatingService.rateEpisode(SERIES_TMDB_ID, 1, 1, request(9.0));

        assertThat(response.seriesTmdbId()).isEqualTo(SERIES_TMDB_ID);
        assertThat(response.seasonNumber()).isEqualTo(1);
        assertThat(response.episodeNumber()).isEqualTo(1);
        assertThat(response.value()).isEqualTo(9.0);

        ArgumentCaptor<MediaItemSnapshot> snapshot = ArgumentCaptor.forClass(MediaItemSnapshot.class);
        verify(mediaItemService).findOrCreate(snapshot.capture());
        assertThat(snapshot.getValue().mediaType()).isEqualTo(MediaType.TV);
        assertThat(snapshot.getValue().tmdbId()).isEqualTo(SERIES_TMDB_ID);
    }

    @Test
    void rateEpisodeUpdatesExistingRating() {
        EpisodeRating existingRating = episodeRating(5.0);
        when(mediaItemService.findOrCreate(any())).thenReturn(seriesItem());
        whenFindingEpisodeRating().thenReturn(Optional.of(existingRating));
        when(episodeRatingRepository.saveAndFlush(existingRating)).thenReturn(existingRating);

        var response = episodeRatingService.rateEpisode(SERIES_TMDB_ID, 1, 1, request(7.5));

        assertThat(response.value()).isEqualTo(7.5);
        assertThat(existingRating.getValue()).isEqualTo(7.5);
    }

    @Test
    void deleteEpisodeRatingDeletesExistingRating() {
        EpisodeRating episodeRating = episodeRating(9.0);
        whenFindingEpisodeRating().thenReturn(Optional.of(episodeRating));

        episodeRatingService.deleteEpisodeRating(SERIES_TMDB_ID, 1, 1);

        verify(episodeRatingRepository).delete(episodeRating);
    }

    @Test
    void deleteEpisodeRatingRejectsMissingRating() {
        whenFindingEpisodeRating().thenReturn(Optional.empty());

        assertThatThrownBy(() -> episodeRatingService.deleteEpisodeRating(SERIES_TMDB_ID, 1, 1))
                .isInstanceOf(EpisodeRatingNotFoundException.class);
    }

    private org.mockito.stubbing.OngoingStubbing<Optional<EpisodeRating>> whenFindingEpisodeRating() {
        return when(episodeRatingRepository
                .findByUser_IdAndSeriesItem_MediaTypeAndSeriesItem_TmdbIdAndSeasonNumberAndEpisodeNumber(
                        USER_ID, MediaType.TV, SERIES_TMDB_ID, 1, 1));
    }

    private EpisodeRating episodeRating(double value) {
        return new EpisodeRating(user, seriesItem(), 1, 1, "Pilot", null, LocalDate.of(2008, 1, 20), value);
    }

    private static MediaItem seriesItem() {
        return new MediaItem(SERIES_TMDB_ID, MediaType.TV, "Breaking Bad", null, null, null, null, 8.9, 15000);
    }

    private static EpisodeRatingRequest request(double value) {
        return new EpisodeRatingRequest(
                value,
                "Pilot",
                null,
                LocalDate.of(2008, 1, 20),
                new SeriesSnapshotRequest("Breaking Bad", null, null, null, null, 8.9, 15000)
        );
    }
}
