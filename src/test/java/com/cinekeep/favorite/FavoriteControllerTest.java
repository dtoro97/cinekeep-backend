package com.cinekeep.favorite;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaType;
import com.cinekeep.auth.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
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

@WebMvcTest(FavoriteController.class)
@Import(SecurityConfig.class)
@WithMockUser
class FavoriteControllerTest {
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
    private FavoriteService favoriteService;

    @Test
    void getFavoritesUsesDefaultPagingAndSorting() {
        when(favoriteService.getFavorites(null, 0, 20, Sort.Direction.DESC))
                .thenReturn(new PageResponse<>(List.of(favoriteItemResponse()), 0, 20, 1, 1));

        assertThat(mockMvc.get().uri("/api/favorites"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].tmdbId", value -> value.assertThat().isEqualTo(977942))
                .hasPathSatisfying("$.content[0].mediaType", value -> value.assertThat().isEqualTo("movie"))
                .hasPathSatisfying("$.totalElements", value -> value.assertThat().isEqualTo(1));
    }

    @Test
    void getFavoritesPassesFilterPagingAndSorting() {
        when(favoriteService.getFavorites(MediaType.TV, 2, 10, Sort.Direction.ASC))
                .thenReturn(new PageResponse<>(List.of(), 2, 10, 0, 0));

        assertThat(mockMvc.get().uri("/api/favorites?mediaType=tv&page=2&size=10&sortDirection=asc"))
                .hasStatusOk();

        verify(favoriteService).getFavorites(MediaType.TV, 2, 10, Sort.Direction.ASC);
    }

    @Test
    void getFavoritesRejectsInvalidPageSize() {
        assertThat(mockMvc.get().uri("/api/favorites?size=0"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.status", value -> value.assertThat().isEqualTo(400))
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("size"));

        verifyNoInteractions(favoriteService);
    }

    @Test
    void getFavoritesRejectsUnknownMediaType() {
        assertThat(mockMvc.get().uri("/api/favorites?mediaType=person"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.status", value -> value.assertThat().isEqualTo(400));

        verifyNoInteractions(favoriteService);
    }

    @Test
    void addToFavoritesReturnsCreatedItem() {
        when(favoriteService.addToFavorites(any())).thenReturn(favoriteItemResponse());

        assertThat(mockMvc.post().uri("/api/favorites")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .hasStatus(201)
                .bodyJson()
                .hasPathSatisfying("$.title", value -> value.assertThat().isEqualTo("The Uprising"));
    }

    @Test
    void addToFavoritesRejectsMissingTitle() {
        assertThat(mockMvc.post().uri("/api/favorites")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "tmdbId": 977942, "mediaType": "movie" }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("title"));

        verifyNoInteractions(favoriteService);
    }

    @Test
    void addToFavoritesRejectsMalformedBody() {
        assertThat(mockMvc.post().uri("/api/favorites")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{ not json"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.status", value -> value.assertThat().isEqualTo(400));
    }

    @Test
    void addToFavoritesReturnsConflictForDuplicate() {
        when(favoriteService.addToFavorites(any())).thenThrow(new FavoriteItemAlreadyExistsException());

        assertThat(mockMvc.post().uri("/api/favorites")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .hasStatus(409)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 409, "message": "Media item is already in favorites" }
                        """);
    }

    @Test
    void removeFromFavoritesReturnsNoContent() {
        assertThat(mockMvc.delete().uri("/api/favorites/movie/977942"))
                .hasStatus(204);

        verify(favoriteService).removeFromFavorites(MediaType.MOVIE, 977942);
    }

    @Test
    void removeFromFavoritesReturnsNotFoundForMissingItem() {
        doThrow(new FavoriteItemNotFoundException())
                .when(favoriteService).removeFromFavorites(MediaType.MOVIE, 1);

        assertThat(mockMvc.delete().uri("/api/favorites/movie/1"))
                .hasStatus(404)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 404, "message": "Favorite item not found" }
                        """);
    }

    private static FavoriteItemResponse favoriteItemResponse() {
        return new FavoriteItemResponse(
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
