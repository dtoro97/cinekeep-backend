package com.cinekeep.rating;

import com.cinekeep.common.PageResponse;
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

@WebMvcTest(EpisodeRatingController.class)
@Import(SecurityConfig.class)
@WithMockUser
class EpisodeRatingControllerTest {
    private static final String EPISODE_URI = "/api/ratings/tv/1396/seasons/1/episodes/1";
    private static final String VALID_REQUEST = """
            {
              "value": 9.0,
              "episodeName": "Pilot",
              "airDate": "2008-01-20",
              "series": { "title": "Breaking Bad" }
            }
            """;

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private EpisodeRatingService episodeRatingService;

    @Test
    void getEpisodeRatingsUsesDefaultPagingAndSorting() {
        when(episodeRatingService.getEpisodeRatings(0, 20, Sort.Direction.DESC))
                .thenReturn(new PageResponse<>(List.of(episodeRatingResponse()), 0, 20, 1, 1));

        assertThat(mockMvc.get().uri("/api/ratings/episodes"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].seriesTitle", value -> value.assertThat().isEqualTo("Breaking Bad"))
                .hasPathSatisfying("$.content[0].episodeNumber", value -> value.assertThat().isEqualTo(1));
    }

    @Test
    void getEpisodeRatingReturnsRating() {
        when(episodeRatingService.getEpisodeRating(1396, 1, 1)).thenReturn(episodeRatingResponse());

        assertThat(mockMvc.get().uri(EPISODE_URI))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.value", value -> value.assertThat().isEqualTo(9.0));
    }

    @Test
    void getEpisodeRatingReturnsNotFoundWhenNotRated() {
        when(episodeRatingService.getEpisodeRating(1396, 1, 1)).thenThrow(new EpisodeRatingNotFoundException());

        assertThat(mockMvc.get().uri(EPISODE_URI))
                .hasStatus(404)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 404, "message": "Episode rating not found" }
                        """);
    }

    @Test
    void rateEpisodeReturnsSavedRating() {
        when(episodeRatingService.rateEpisode(eq(1396), eq(1), eq(1), any())).thenReturn(episodeRatingResponse());

        assertThat(mockMvc.put().uri(EPISODE_URI)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.episodeName", value -> value.assertThat().isEqualTo("Pilot"));
    }

    @Test
    void rateEpisodeAcceptsSpecialsSeasonZero() {
        when(episodeRatingService.rateEpisode(eq(1396), eq(0), eq(1), any())).thenReturn(episodeRatingResponse());

        assertThat(mockMvc.put().uri("/api/ratings/tv/1396/seasons/0/episodes/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .hasStatusOk();
    }

    @Test
    void rateEpisodeRejectsMissingSeriesTitle() {
        assertThat(mockMvc.put().uri(EPISODE_URI)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "value": 9.0, "episodeName": "Pilot", "series": {} }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat()
                        .isEqualTo("series.title: must not be blank"));

        verifyNoInteractions(episodeRatingService);
    }

    @Test
    void rateEpisodeRejectsInvalidValue() {
        assertThat(mockMvc.put().uri(EPISODE_URI)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "value": 0, "episodeName": "Pilot", "series": { "title": "Breaking Bad" } }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("value"));

        verifyNoInteractions(episodeRatingService);
    }

    @Test
    void rateEpisodeRejectsEpisodeNumberZero() {
        assertThat(mockMvc.put().uri("/api/ratings/tv/1396/seasons/1/episodes/0")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("episodeNumber"));

        verifyNoInteractions(episodeRatingService);
    }

    @Test
    void deleteEpisodeRatingReturnsNoContent() {
        assertThat(mockMvc.delete().uri(EPISODE_URI))
                .hasStatus(204);

        verify(episodeRatingService).deleteEpisodeRating(1396, 1, 1);
    }

    @Test
    void deleteEpisodeRatingReturnsNotFoundWhenNotRated() {
        doThrow(new EpisodeRatingNotFoundException())
                .when(episodeRatingService).deleteEpisodeRating(1396, 1, 1);

        assertThat(mockMvc.delete().uri(EPISODE_URI))
                .hasStatus(404);
    }

    private static EpisodeRatingResponse episodeRatingResponse() {
        Instant ratedAt = Instant.parse("2026-09-30T10:00:00Z");

        return new EpisodeRatingResponse(
                1L,
                1396,
                "Breaking Bad",
                null,
                1,
                1,
                "Pilot",
                null,
                LocalDate.of(2008, 1, 20),
                9.0,
                ratedAt,
                ratedAt
        );
    }
}
