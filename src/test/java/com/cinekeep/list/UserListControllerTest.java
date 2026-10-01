package com.cinekeep.list;

import com.cinekeep.common.PageResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(UserListController.class)
@Import(SecurityConfig.class)
@WithMockUser
class UserListControllerTest {
    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private UserListService userListService;

    @Test
    void getListsReturnsPage() {
        when(userListService.getLists(0, 20))
                .thenReturn(new PageResponse<>(List.of(userListResponse()), 0, 20, 1, 1));

        assertThat(mockMvc.get().uri("/api/lists"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].name", value -> value.assertThat().isEqualTo("Rainy Sunday"))
                .hasPathSatisfying("$.content[0].sortBy", value -> value.assertThat().isEqualTo("title.asc"))
                .hasPathSatisfying("$.content[0].cover.selected", value -> value.assertThat().isEqualTo(true));
    }

    @Test
    void getMembershipReturnsListIds() {
        when(userListService.getMembership(MediaType.MOVIE, 550)).thenReturn(new ListMembershipResponse(List.of(1L, 4L)));

        assertThat(mockMvc.get().uri("/api/lists/membership?mediaType=movie&tmdbId=550"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "listIds": [1, 4] }
                        """);
    }

    @Test
    void createListReturnsCreatedList() {
        when(userListService.createList(any())).thenReturn(userListResponse());

        assertThat(mockMvc.post().uri("/api/lists")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Rainy Sunday", "sortBy": "title.asc" }
                        """))
                .hasStatus(201)
                .bodyJson()
                .hasPathSatisfying("$.id", value -> value.assertThat().isEqualTo(1));
    }

    @Test
    void createListRejectsBlankName() {
        assertThat(mockMvc.post().uri("/api/lists")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "name": " " }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("name"));

        verifyNoInteractions(userListService);
    }

    @Test
    void getListDetailsPassesSortOverride() {
        when(userListService.getListDetails(1L, 0, 20, ListSortBy.VOTE_AVERAGE_DESC))
                .thenReturn(new UserListDetailsResponse(
                        userListResponse(),
                        ListSortBy.VOTE_AVERAGE_DESC,
                        new PageResponse<>(List.of(), 0, 20, 0, 0)
                ));

        assertThat(mockMvc.get().uri("/api/lists/1?sortBy=vote_average.desc"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.appliedSortBy", value -> value.assertThat().isEqualTo("vote_average.desc"));
    }

    @Test
    void getListDetailsUsesStoredSortWhenNoOverride() {
        when(userListService.getListDetails(1L, 0, 20, null))
                .thenReturn(new UserListDetailsResponse(
                        userListResponse(),
                        ListSortBy.TITLE_ASC,
                        new PageResponse<>(List.of(), 0, 20, 0, 0)
                ));

        assertThat(mockMvc.get().uri("/api/lists/1")).hasStatusOk();

        verify(userListService).getListDetails(1L, 0, 20, null);
    }

    @Test
    void getListDetailsRejectsUnknownSort() {
        assertThat(mockMvc.get().uri("/api/lists/1?sortBy=popularity.desc"))
                .hasStatus(400);

        verifyNoInteractions(userListService);
    }

    @Test
    void getListDetailsReturnsNotFoundForUnknownList() {
        when(userListService.getListDetails(99L, 0, 20, null)).thenThrow(new UserListNotFoundException());

        assertThat(mockMvc.get().uri("/api/lists/99"))
                .hasStatus(404)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 404, "message": "List not found" }
                        """);
    }

    @Test
    void updateListReturnsUpdatedList() {
        when(userListService.updateList(eq(1L), any())).thenReturn(userListResponse());

        assertThat(mockMvc.put().uri("/api/lists/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Rainy Sunday",
                          "isPublic": true,
                          "sortBy": "title.asc",
                          "cover": { "mediaType": "movie", "tmdbId": 550 }
                        }
                        """))
                .hasStatusOk();
    }

    @Test
    void updateListRejectsIncompleteCover() {
        assertThat(mockMvc.put().uri("/api/lists/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Rainy Sunday", "isPublic": true, "sortBy": "title.asc", "cover": { "mediaType": "movie" } }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString().contains("cover.tmdbId"));

        verifyNoInteractions(userListService);
    }

    @Test
    void updateListReturnsBadRequestWhenCoverIsNotInList() {
        when(userListService.updateList(eq(1L), any())).thenThrow(new CoverItemNotInListException());

        assertThat(mockMvc.put().uri("/api/lists/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Rainy Sunday", "isPublic": true, "sortBy": "title.asc", "cover": { "mediaType": "movie", "tmdbId": 13 } }
                        """))
                .hasStatus(400)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 400, "message": "Cover must be an item in the list" }
                        """);
    }

    @Test
    void deleteListReturnsNoContent() {
        assertThat(mockMvc.delete().uri("/api/lists/1")).hasStatus(204);

        verify(userListService).deleteList(1L);
    }

    @Test
    void deleteListReturnsNotFoundForUnknownList() {
        doThrow(new UserListNotFoundException()).when(userListService).deleteList(99L);

        assertThat(mockMvc.delete().uri("/api/lists/99")).hasStatus(404);
    }

    private static UserListResponse userListResponse() {
        Instant createdAt = Instant.parse("2026-10-01T10:00:00Z");

        return new UserListResponse(
                1L,
                "Rainy Sunday",
                null,
                true,
                ListSortBy.TITLE_ASC,
                2,
                new ListCoverResponse(550, MediaType.MOVIE, "/poster.jpg", "/backdrop.jpg", true),
                createdAt,
                createdAt
        );
    }
}
