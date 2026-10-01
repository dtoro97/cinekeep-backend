package com.cinekeep.rating;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(RatingController.class)
@Import(SecurityConfig.class)
@WithMockUser
class RatingControllerTest {
    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private RatingService ratingService;

    @Test
    void getRatingsUsesDefaultPagingAndSorting() {
        when(ratingService.getRatings(null, 0, 20, Sort.Direction.DESC))
                .thenReturn(new PageResponse<>(List.of(ratingResponse(8.0)), 0, 20, 1, 1));

        assertThat(mockMvc.get().uri("/api/ratings"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].value", value -> value.assertThat().isEqualTo(8.0))
                .hasPathSatisfying("$.totalElements", value -> value.assertThat().isEqualTo(1));
    }

    @Test
    void getRatingsPassesFilterPagingAndSorting() {
        when(ratingService.getRatings(MediaType.TV, 1, 5, Sort.Direction.ASC))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0));

        assertThat(mockMvc.get().uri("/api/ratings?mediaType=tv&page=1&size=5&sortDirection=asc"))
                .hasStatusOk();

        verify(ratingService).getRatings(MediaType.TV, 1, 5, Sort.Direction.ASC);
    }

    @Test
    void getRatingReturnsRating() {
        when(ratingService.getRating(MediaType.MOVIE, 550)).thenReturn(ratingResponse(8.0));

        assertThat(mockMvc.get().uri("/api/ratings/movie/550"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.value", value -> value.assertThat().isEqualTo(8.0));
    }

    @Test
    void getRatingReturnsNotFoundWhenNotRated() {
        when(ratingService.getRating(MediaType.MOVIE, 550)).thenThrow(new RatingNotFoundException());

        assertThat(mockMvc.get().uri("/api/ratings/movie/550"))
                .hasStatus(404)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 404, "message": "Rating not found" }
                        """);
    }

    @Test
    void rateReturnsSavedRating() {
        when(ratingService.rate(eq(MediaType.MOVIE), eq(550), any())).thenReturn(ratingResponse(6.5));

        assertThat(mockMvc.put().uri("/api/ratings/movie/550")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "value": 6.5, "title": "Fight Club" }
                        """))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.value", value -> value.assertThat().isEqualTo(6.5));
    }

    @Test
    void rateRejectsValueOutsideHalfSteps() {
        assertThat(mockMvc.put().uri("/api/ratings/movie/550")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "value": 7.3, "title": "Fight Club" }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat()
                        .isEqualTo("value: must be between 0.5 and 10 in steps of 0.5"));

        verifyNoInteractions(ratingService);
    }

    @Test
    void rateRejectsMissingValueAndTitle() {
        assertThat(mockMvc.put().uri("/api/ratings/movie/550")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString()
                        .contains("title").contains("value"));

        verifyNoInteractions(ratingService);
    }

    @Test
    void rateRejectsNonPositiveTmdbId() {
        assertThat(mockMvc.put().uri("/api/ratings/movie/0")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "value": 6.5, "title": "Fight Club" }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("tmdbId"));

        verifyNoInteractions(ratingService);
    }

    @Test
    void deleteRatingReturnsNoContent() {
        assertThat(mockMvc.delete().uri("/api/ratings/tv/1396"))
                .hasStatus(204);

        verify(ratingService).deleteRating(MediaType.TV, 1396);
    }

    @Test
    void deleteRatingReturnsNotFoundWhenNotRated() {
        doThrow(new RatingNotFoundException()).when(ratingService).deleteRating(MediaType.MOVIE, 550);

        assertThat(mockMvc.delete().uri("/api/ratings/movie/550"))
                .hasStatus(404);
    }

    private static RatingResponse ratingResponse(double value) {
        Instant ratedAt = Instant.parse("2026-09-30T10:00:00Z");

        return new RatingResponse(
                1L,
                550,
                MediaType.MOVIE,
                "Fight Club",
                null,
                null,
                null,
                LocalDate.of(1999, 10, 15),
                8.4,
                30000,
                value,
                ratedAt,
                ratedAt
        );
    }
}
