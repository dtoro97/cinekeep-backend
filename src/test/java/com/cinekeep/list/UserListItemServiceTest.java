package com.cinekeep.list;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaItemService;
import com.cinekeep.media.MediaType;
import com.cinekeep.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserListItemServiceTest {
    private static final long LIST_ID = 10L;

    @Mock
    private UserListService userListService;

    @Mock
    private MediaItemService mediaItemService;

    @Mock
    private UserListItemRepository userListItemRepository;

    @InjectMocks
    private UserListItemService userListItemService;

    private UserList userList;
    private MediaItem fightClub;

    @BeforeEach
    void setUp() {
        userList = new UserList(new User("dev@cinekeep.test", "dev", "password-hash"), "Rainy Sunday", null, false, ListSortBy.ORIGINAL_ORDER_ASC);
        ReflectionTestUtils.setField(userList, "id", LIST_ID);
        fightClub = new MediaItem(550, MediaType.MOVIE, "Fight Club", null, null, null, null, 8.4, 30000);
        ReflectionTestUtils.setField(fightClub, "id", 1L);
        when(userListService.getOwnedList(LIST_ID)).thenReturn(userList);
    }

    @Test
    void addItemSavesItemWithNormalizedComment() {
        when(userListItemRepository.existsByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                LIST_ID, MediaType.MOVIE, 550)).thenReturn(false);
        when(mediaItemService.findOrCreate(any())).thenReturn(fightClub);
        when(userListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserListItemResponse response = userListItemService.addItem(LIST_ID, request("  rewatch  "));

        assertThat(response.tmdbId()).isEqualTo(550);
        assertThat(response.comment()).isEqualTo("rewatch");
    }

    @Test
    void addItemRejectsDuplicate() {
        when(userListItemRepository.existsByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                LIST_ID, MediaType.MOVIE, 550)).thenReturn(true);

        assertThatThrownBy(() -> userListItemService.addItem(LIST_ID, request(null)))
                .isInstanceOf(ListItemAlreadyExistsException.class);

        verify(userListItemRepository, never()).save(any());
    }

    @Test
    void updateItemClearsBlankComment() {
        UserListItem item = new UserListItem(userList, fightClub, "old note");
        whenFindingItem().thenReturn(Optional.of(item));

        UserListItemResponse response = userListItemService.updateItem(
                LIST_ID, MediaType.MOVIE, 550, new UpdateListItemRequest("   ")
        );

        assertThat(response.comment()).isNull();
    }

    @Test
    void removeItemClearsCoverWhenRemovingCoverItem() {
        userList.update("Rainy Sunday", null, false, ListSortBy.ORIGINAL_ORDER_ASC, fightClub);
        UserListItem item = new UserListItem(userList, fightClub, null);
        whenFindingItem().thenReturn(Optional.of(item));

        userListItemService.removeItem(LIST_ID, MediaType.MOVIE, 550);

        assertThat(userList.getCoverMediaItem()).isNull();
        verify(userListItemRepository).delete(item);
    }

    @Test
    void removeItemKeepsCoverWhenRemovingOtherItem() {
        MediaItem breakingBad = new MediaItem(1396, MediaType.TV, "Breaking Bad", null, null, null, null, 8.9, 15000);
        ReflectionTestUtils.setField(breakingBad, "id", 2L);
        userList.update("Rainy Sunday", null, false, ListSortBy.ORIGINAL_ORDER_ASC, breakingBad);
        whenFindingItem().thenReturn(Optional.of(new UserListItem(userList, fightClub, null)));

        userListItemService.removeItem(LIST_ID, MediaType.MOVIE, 550);

        assertThat(userList.getCoverMediaItem()).isSameAs(breakingBad);
    }

    @Test
    void removeItemRejectsMissingItem() {
        whenFindingItem().thenReturn(Optional.empty());

        assertThatThrownBy(() -> userListItemService.removeItem(LIST_ID, MediaType.MOVIE, 550))
                .isInstanceOf(ListItemNotFoundException.class);
    }

    @Test
    void clearItemsDeletesItemsAndCover() {
        userList.update("Rainy Sunday", null, false, ListSortBy.ORIGINAL_ORDER_ASC, fightClub);

        userListItemService.clearItems(LIST_ID);

        assertThat(userList.getCoverMediaItem()).isNull();
        verify(userListItemRepository).deleteAllByListId(LIST_ID);
    }

    private org.mockito.stubbing.OngoingStubbing<Optional<UserListItem>> whenFindingItem() {
        return when(userListItemRepository.findByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                LIST_ID, MediaType.MOVIE, 550));
    }

    private static ListItemRequest request(String comment) {
        return new ListItemRequest(550, MediaType.MOVIE, "Fight Club", null, null, null, null, 8.4, 30000, comment);
    }
}
