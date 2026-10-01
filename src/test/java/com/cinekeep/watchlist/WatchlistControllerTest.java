package com.cinekeep.watchlist;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(WatchlistController.class)
class WatchlistControllerTest {
    private static final String VALID_REQUEST = """
            {
              "tmdbId": 977942,
              "mediaType": "movie",
              "title": "The Uprising",
              "releaseDate": "2026-09-10",
              "voteAverage": 7.5,
              "voteCount": 32
            }
            """;

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private WatchlistService watchlistService;

    @Test
    void getWatchlistUsesDefaultPagingAndSorting() {
        when(watchlistService.getWatchlist(null, 0, 20, Sort.Direction.DESC))
                .thenReturn(new PageResponse<>(List.of(watchlistItemResponse()), 0, 20, 1, 1));

        assertThat(mockMvc.get().uri("/api/watchlist"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].tmdbId", value -> value.assertThat().isEqualTo(977942))
                .hasPathSatisfying("$.content[0].mediaType", value -> value.assertThat().isEqualTo("movie"))
                .hasPathSatisfying("$.totalElements", value -> value.assertThat().isEqualTo(1));
    }

    @Test
    void getWatchlistPassesFilterPagingAndSorting() {
        when(watchlistService.getWatchlist(MediaType.TV, 2, 10, Sort.Direction.ASC))
                .thenReturn(new PageResponse<>(List.of(), 2, 10, 0, 0));

        assertThat(mockMvc.get().uri("/api/watchlist?mediaType=tv&page=2&size=10&sortDirection=asc"))
                .hasStatusOk();

        verify(watchlistService).getWatchlist(MediaType.TV, 2, 10, Sort.Direction.ASC);
    }

    @Test
    void getWatchlistRejectsInvalidPageSize() {
        assertThat(mockMvc.get().uri("/api/watchlist?size=0"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.status", value -> value.assertThat().isEqualTo(400))
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("size"));

        verifyNoInteractions(watchlistService);
    }

    @Test
    void getWatchlistRejectsUnknownMediaType() {
        assertThat(mockMvc.get().uri("/api/watchlist?mediaType=person"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.status", value -> value.assertThat().isEqualTo(400));

        verifyNoInteractions(watchlistService);
    }

    @Test
    void addToWatchlistReturnsCreatedItem() {
        when(watchlistService.addToWatchlist(any())).thenReturn(watchlistItemResponse());

        assertThat(mockMvc.post().uri("/api/watchlist")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .hasStatus(201)
                .bodyJson()
                .hasPathSatisfying("$.title", value -> value.assertThat().isEqualTo("The Uprising"));
    }

    @Test
    void addToWatchlistRejectsMissingTitle() {
        assertThat(mockMvc.post().uri("/api/watchlist")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "tmdbId": 977942, "mediaType": "movie" }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("title"));

        verifyNoInteractions(watchlistService);
    }

    @Test
    void addToWatchlistRejectsMalformedBody() {
        assertThat(mockMvc.post().uri("/api/watchlist")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{ not json"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.status", value -> value.assertThat().isEqualTo(400));
    }

    @Test
    void addToWatchlistReturnsConflictForDuplicate() {
        when(watchlistService.addToWatchlist(any())).thenThrow(new WatchlistItemAlreadyExistsException());

        assertThat(mockMvc.post().uri("/api/watchlist")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .hasStatus(409)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 409, "message": "Media item is already in the watchlist" }
                        """);
    }

    @Test
    void removeFromWatchlistReturnsNoContent() {
        assertThat(mockMvc.delete().uri("/api/watchlist/movie/977942"))
                .hasStatus(204);

        verify(watchlistService).removeFromWatchlist(MediaType.MOVIE, 977942);
    }

    @Test
    void removeFromWatchlistReturnsNotFoundForMissingItem() {
        doThrow(new WatchlistItemNotFoundException())
                .when(watchlistService).removeFromWatchlist(MediaType.MOVIE, 1);

        assertThat(mockMvc.delete().uri("/api/watchlist/movie/1"))
                .hasStatus(404)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 404, "message": "Watchlist item not found" }
                        """);
    }

    private static WatchlistItemResponse watchlistItemResponse() {
        return new WatchlistItemResponse(
                1L,
                977942,
                MediaType.MOVIE,
                "The Uprising",
                null,
                null,
                null,
                LocalDate.of(2026, 9, 10),
                7.5,
                32,
                Instant.parse("2026-09-30T10:00:00Z")
        );
    }
}
