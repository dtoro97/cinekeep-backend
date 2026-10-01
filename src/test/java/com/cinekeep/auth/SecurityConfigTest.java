package com.cinekeep.auth;

import com.cinekeep.common.PageResponse;
import com.cinekeep.watchlist.WatchlistController;
import com.cinekeep.watchlist.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(WatchlistController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private WatchlistService watchlistService;

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() {
        assertThat(mockMvc.get().uri("/api/watchlist"))
                .hasStatus(401)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 401, "message": "Authentication required" }
                        """);

        verifyNoInteractions(watchlistService);
    }

    @Test
    void protectedEndpointWithInvalidTokenReturnsUnauthorized() {
        assertThat(mockMvc.get().uri("/api/watchlist")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .hasStatus(401);

        verifyNoInteractions(watchlistService);
    }

    @Test
    void protectedEndpointWithJwtIsAllowed() {
        when(watchlistService.getWatchlist(null, 0, 20, Sort.Direction.DESC))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        assertThat(mockMvc.get().uri("/api/watchlist").with(jwt().jwt(token -> token.subject("1"))))
                .hasStatusOk();
    }
}
