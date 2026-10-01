package com.cinekeep.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaItemServiceTest {
    @Mock
    private MediaItemRepository mediaItemRepository;

    @InjectMocks
    private MediaItemService mediaItemService;

    @Test
    void findOrCreateCreatesMissingMediaItem() {
        when(mediaItemRepository.findByMediaTypeAndTmdbId(MediaType.MOVIE, 550)).thenReturn(Optional.empty());
        when(mediaItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MediaItem mediaItem = mediaItemService.findOrCreate(new MediaItemSnapshot(
                550, MediaType.MOVIE, "Fight Club", "/poster.jpg", null, null, LocalDate.of(1999, 10, 15), 8.4, 30000
        ));

        assertThat(mediaItem.getTitle()).isEqualTo("Fight Club");
        assertThat(mediaItem.getPosterPath()).isEqualTo("/poster.jpg");
        assertThat(mediaItem.getBackdropPath()).isNull();
    }

    @Test
    void findOrCreateKeepsExistingValuesForMissingSnapshotFields() {
        MediaItem existing = new MediaItem(
                550, MediaType.MOVIE, "Fight Club", "/poster.jpg", "/backdrop.jpg", "Overview",
                LocalDate.of(1999, 10, 15), 8.4, 30000
        );
        when(mediaItemRepository.findByMediaTypeAndTmdbId(MediaType.MOVIE, 550)).thenReturn(Optional.of(existing));

        MediaItem mediaItem = mediaItemService.findOrCreate(new MediaItemSnapshot(
                550, MediaType.MOVIE, "Fight Club", null, null, null, null, null, null
        ));

        assertThat(mediaItem.getPosterPath()).isEqualTo("/poster.jpg");
        assertThat(mediaItem.getBackdropPath()).isEqualTo("/backdrop.jpg");
        assertThat(mediaItem.getOverview()).isEqualTo("Overview");
        assertThat(mediaItem.getReleaseDate()).isEqualTo(LocalDate.of(1999, 10, 15));
        assertThat(mediaItem.getVoteAverage()).isEqualTo(8.4);
        assertThat(mediaItem.getVoteCount()).isEqualTo(30000);
        verify(mediaItemRepository, never()).save(any());
    }

    @Test
    void findOrCreateUpdatesProvidedSnapshotFields() {
        MediaItem existing = new MediaItem(
                550, MediaType.MOVIE, "Fight Club", "/old-poster.jpg", null, null, null, 8.4, 30000
        );
        when(mediaItemRepository.findByMediaTypeAndTmdbId(MediaType.MOVIE, 550)).thenReturn(Optional.of(existing));

        MediaItem mediaItem = mediaItemService.findOrCreate(new MediaItemSnapshot(
                550, MediaType.MOVIE, "Fight Club", "/new-poster.jpg", "/backdrop.jpg", null, null, 8.5, 31000
        ));

        assertThat(mediaItem.getPosterPath()).isEqualTo("/new-poster.jpg");
        assertThat(mediaItem.getBackdropPath()).isEqualTo("/backdrop.jpg");
        assertThat(mediaItem.getVoteAverage()).isEqualTo(8.5);
        assertThat(mediaItem.getVoteCount()).isEqualTo(31000);
    }
}
