package com.cinekeep.media;

import com.cinekeep.auth.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(MediaStateController.class)
@Import(SecurityConfig.class)
@WithMockUser
class MediaStateControllerTest {
    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private MediaStateService mediaStateService;

    @Test
    void getMediaStateReturnsCombinedState() {
        when(mediaStateService.getMediaState(MediaType.MOVIE, 550))
                .thenReturn(new MediaStateResponse(true, false, 8.5));

        assertThat(mockMvc.get().uri("/api/media/movie/550/state"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "inWatchlist": true, "favorite": false, "rating": 8.5 }
                        """);
    }

    @Test
    void getMediaStateReturnsNullRatingWhenNotRated() {
        when(mediaStateService.getMediaState(MediaType.TV, 1396))
                .thenReturn(new MediaStateResponse(false, false, null));

        assertThat(mockMvc.get().uri("/api/media/tv/1396/state"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "inWatchlist": false, "favorite": false, "rating": null }
                        """);
    }

    @Test
    void getMediaStateRejectsUnknownMediaType() {
        assertThat(mockMvc.get().uri("/api/media/person/550/state"))
                .hasStatus(400);

        verifyNoInteractions(mediaStateService);
    }

    @Test
    void getMediaStateRejectsNonPositiveTmdbId() {
        assertThat(mockMvc.get().uri("/api/media/movie/0/state"))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("tmdbId"));

        verifyNoInteractions(mediaStateService);
    }
}
