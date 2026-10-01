package com.cinekeep.list;

import com.cinekeep.media.MediaType;
import com.cinekeep.auth.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(UserListItemController.class)
@Import(SecurityConfig.class)
@WithMockUser
class UserListItemControllerTest {
    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private UserListItemService userListItemService;

    @Test
    void addItemReturnsCreatedItem() {
        when(userListItemService.addItem(eq(1L), any())).thenReturn(itemResponse("rewatch"));

        assertThat(mockMvc.post().uri("/api/lists/1/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "tmdbId": 550, "mediaType": "movie", "title": "Fight Club", "comment": "rewatch" }
                        """))
                .hasStatus(201)
                .bodyJson()
                .hasPathSatisfying("$.comment", value -> value.assertThat().isEqualTo("rewatch"));
    }

    @Test
    void addItemRejectsMissingTitle() {
        assertThat(mockMvc.post().uri("/api/lists/1/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "tmdbId": 550, "mediaType": "movie" }
                        """))
                .hasStatus(400);

        verifyNoInteractions(userListItemService);
    }

    @Test
    void addItemReturnsConflictForDuplicate() {
        when(userListItemService.addItem(eq(1L), any())).thenThrow(new ListItemAlreadyExistsException());

        assertThat(mockMvc.post().uri("/api/lists/1/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "tmdbId": 550, "mediaType": "movie", "title": "Fight Club" }
                        """))
                .hasStatus(409)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 409, "message": "Media item is already in the list" }
                        """);
    }

    @Test
    void updateItemReturnsUpdatedItem() {
        when(userListItemService.updateItem(eq(1L), eq(MediaType.MOVIE), eq(550), any()))
                .thenReturn(itemResponse("new note"));

        assertThat(mockMvc.put().uri("/api/lists/1/items/movie/550")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "comment": "new note" }
                        """))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.comment", value -> value.assertThat().isEqualTo("new note"));
    }

    @Test
    void removeItemReturnsNoContent() {
        assertThat(mockMvc.delete().uri("/api/lists/1/items/tv/1396")).hasStatus(204);

        verify(userListItemService).removeItem(1L, MediaType.TV, 1396);
    }

    @Test
    void removeItemReturnsNotFoundForMissingItem() {
        doThrow(new ListItemNotFoundException()).when(userListItemService).removeItem(1L, MediaType.MOVIE, 550);

        assertThat(mockMvc.delete().uri("/api/lists/1/items/movie/550"))
                .hasStatus(404)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 404, "message": "List item not found" }
                        """);
    }

    @Test
    void clearItemsReturnsNoContent() {
        assertThat(mockMvc.delete().uri("/api/lists/1/items")).hasStatus(204);

        verify(userListItemService).clearItems(1L);
    }

    private static UserListItemResponse itemResponse(String comment) {
        return new UserListItemResponse(
                1L,
                550,
                MediaType.MOVIE,
                "Fight Club",
                null,
                null,
                null,
                null,
                8.4,
                30000,
                comment,
                Instant.parse("2026-10-01T10:00:00Z")
        );
    }
}
