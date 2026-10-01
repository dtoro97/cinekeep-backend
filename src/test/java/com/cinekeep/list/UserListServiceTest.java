package com.cinekeep.list;

import com.cinekeep.media.MediaItem;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserListServiceTest {
    private static final long USER_ID = 1L;
    private static final long LIST_ID = 10L;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserListRepository userListRepository;

    @Mock
    private UserListItemRepository userListItemRepository;

    @InjectMocks
    private UserListService userListService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("dev");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void createListAppliesDefaultsAndNormalizesText() {
        when(userListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserListResponse response = userListService.createList(
                new CreateUserListRequest("  Rainy Sunday  ", "   ", null, null)
        );

        assertThat(response.name()).isEqualTo("Rainy Sunday");
        assertThat(response.description()).isNull();
        assertThat(response.isPublic()).isFalse();
        assertThat(response.sortBy()).isEqualTo(ListSortBy.ORIGINAL_ORDER_ASC);
        assertThat(response.itemCount()).isZero();
        assertThat(response.cover()).isNull();
    }

    @Test
    void getListsUsesBatchedCountsAndLatestItemAsFallbackCover() {
        UserList listWithItems = userList(LIST_ID);
        UserList emptyList = userList(11L);
        Page<UserList> page = new PageImpl<>(List.of(listWithItems, emptyList));
        when(userListRepository.findByUser_Id(eq(USER_ID), any())).thenReturn(page);
        when(userListItemRepository.countByListIds(List.of(LIST_ID, 11L)))
                .thenReturn(List.of(itemCount(LIST_ID, 3)));
        when(userListItemRepository.findLatestItemsByListIds(List.of(LIST_ID, 11L)))
                .thenReturn(List.of(new UserListItem(listWithItems, mediaItem(550, MediaType.MOVIE), null)));

        var response = userListService.getLists(0, 20);

        assertThat(response.content()).hasSize(2);
        UserListResponse first = response.content().getFirst();
        assertThat(first.itemCount()).isEqualTo(3);
        assertThat(first.cover().tmdbId()).isEqualTo(550);
        assertThat(first.cover().selected()).isFalse();
        UserListResponse second = response.content().get(1);
        assertThat(second.itemCount()).isZero();
        assertThat(second.cover()).isNull();
    }

    @Test
    void getListDetailsUsesStoredSortWhenNoOverride() {
        UserList userList = userList(LIST_ID);
        when(userListRepository.findByIdAndUser_Id(LIST_ID, USER_ID)).thenReturn(Optional.of(userList));
        when(userListItemRepository.findByUserList_Id(eq(LIST_ID), any())).thenReturn(Page.empty());

        UserListDetailsResponse response = userListService.getListDetails(LIST_ID, 0, 20, null);

        assertThat(response.appliedSortBy()).isEqualTo(ListSortBy.TITLE_ASC);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userListItemRepository).findByUserList_Id(eq(LIST_ID), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 20, ListSortBy.TITLE_ASC.toSort()));
    }

    @Test
    void getListDetailsPrefersSortOverride() {
        UserList userList = userList(LIST_ID);
        when(userListRepository.findByIdAndUser_Id(LIST_ID, USER_ID)).thenReturn(Optional.of(userList));
        when(userListItemRepository.findByUserList_Id(eq(LIST_ID), any())).thenReturn(Page.empty());

        UserListDetailsResponse response = userListService.getListDetails(LIST_ID, 0, 20, ListSortBy.VOTE_AVERAGE_DESC);

        assertThat(response.appliedSortBy()).isEqualTo(ListSortBy.VOTE_AVERAGE_DESC);
    }

    @Test
    void getOwnedListRejectsListOfAnotherUser() {
        when(userListRepository.findByIdAndUser_Id(LIST_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userListService.getListDetails(LIST_ID, 0, 20, null))
                .isInstanceOf(UserListNotFoundException.class);
    }

    @Test
    void updateListSetsCoverFromListItem() {
        UserList userList = userList(LIST_ID);
        MediaItem coverItem = mediaItem(550, MediaType.MOVIE);
        when(userListRepository.findByIdAndUser_Id(LIST_ID, USER_ID)).thenReturn(Optional.of(userList));
        when(userListItemRepository.findByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                LIST_ID, MediaType.MOVIE, 550)).thenReturn(Optional.of(new UserListItem(userList, coverItem, null)));
        when(userListItemRepository.countByUserList_Id(LIST_ID)).thenReturn(1L);

        UserListResponse response = userListService.updateList(LIST_ID, new UpdateUserListRequest(
                "Renamed",
                "Cozy picks",
                true,
                ListSortBy.RELEASE_DATE_DESC,
                new ListCoverRequest(MediaType.MOVIE, 550)
        ));

        assertThat(response.name()).isEqualTo("Renamed");
        assertThat(response.isPublic()).isTrue();
        assertThat(response.sortBy()).isEqualTo(ListSortBy.RELEASE_DATE_DESC);
        assertThat(response.cover().tmdbId()).isEqualTo(550);
        assertThat(response.cover().selected()).isTrue();
    }

    @Test
    void updateListRejectsCoverNotInList() {
        UserList userList = userList(LIST_ID);
        when(userListRepository.findByIdAndUser_Id(LIST_ID, USER_ID)).thenReturn(Optional.of(userList));
        when(userListItemRepository.findByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                LIST_ID, MediaType.MOVIE, 13)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userListService.updateList(LIST_ID, new UpdateUserListRequest(
                "Renamed", null, false, ListSortBy.TITLE_ASC, new ListCoverRequest(MediaType.MOVIE, 13)
        ))).isInstanceOf(CoverItemNotInListException.class);

        verify(userListRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteListRemovesItemsThenList() {
        UserList userList = userList(LIST_ID);
        when(userListRepository.findByIdAndUser_Id(LIST_ID, USER_ID)).thenReturn(Optional.of(userList));

        userListService.deleteList(LIST_ID);

        verify(userListItemRepository).deleteAllByListId(LIST_ID);
        verify(userListRepository).delete(userList);
    }

    @Test
    void getMembershipReturnsListIdsForCurrentUser() {
        when(userListItemRepository.findListIdsContainingMedia(USER_ID, MediaType.TV, 1396))
                .thenReturn(List.of(LIST_ID));

        assertThat(userListService.getMembership(MediaType.TV, 1396).listIds()).containsExactly(LIST_ID);
    }

    private UserList userList(long id) {
        UserList userList = new UserList(user, "Rainy Sunday", null, false, ListSortBy.TITLE_ASC);
        ReflectionTestUtils.setField(userList, "id", id);
        return userList;
    }

    private static MediaItem mediaItem(int tmdbId, MediaType mediaType) {
        MediaItem mediaItem = new MediaItem(tmdbId, mediaType, "Title " + tmdbId, "/poster.jpg", "/backdrop.jpg",
                null, null, 8.0, 100);
        ReflectionTestUtils.setField(mediaItem, "id", (long) tmdbId);
        return mediaItem;
    }

    private static ListItemCount itemCount(long listId, long count) {
        return new ListItemCount() {
            @Override
            public Long getListId() {
                return listId;
            }

            @Override
            public long getItemCount() {
                return count;
            }
        };
    }
}
