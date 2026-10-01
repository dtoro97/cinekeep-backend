package com.cinekeep.list;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaType;
import com.cinekeep.user.CurrentUserService;
import com.cinekeep.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserListService {
    private final CurrentUserService currentUserService;
    private final UserListRepository userListRepository;
    private final UserListItemRepository userListItemRepository;

    public UserListService(
            CurrentUserService currentUserService,
            UserListRepository userListRepository,
            UserListItemRepository userListItemRepository
    ) {
        this.currentUserService = currentUserService;
        this.userListRepository = userListRepository;
        this.userListItemRepository = userListItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserListResponse> getLists(int page, int size) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<UserList> userLists = userListRepository.findByUser_Id(userId, pageable);

        List<Long> listIds = userLists.map(UserList::getId).getContent();
        Map<Long, Long> itemCounts = listIds.isEmpty() ? Map.of() : userListItemRepository.countByListIds(listIds)
                .stream()
                .collect(Collectors.toMap(ListItemCount::getListId, ListItemCount::getItemCount));
        Map<Long, MediaItem> latestMediaItems = listIds.isEmpty() ? Map.of() : userListItemRepository
                .findLatestItemsByListIds(listIds)
                .stream()
                .collect(Collectors.toMap(item -> item.getUserList().getId(), UserListItem::getMediaItem));

        return PageResponse.from(userLists.map(userList -> UserListResponse.from(
                userList,
                itemCounts.getOrDefault(userList.getId(), 0L),
                latestMediaItems.get(userList.getId())
        )));
    }

    @Transactional(readOnly = true)
    public ListMembershipResponse getMembership(MediaType mediaType, Integer tmdbId) {
        Long userId = currentUserService.getCurrentUser().getId();

        return new ListMembershipResponse(
                userListItemRepository.findListIdsContainingMedia(userId, mediaType, tmdbId)
        );
    }

    @Transactional
    public UserListResponse createList(CreateUserListRequest request) {
        User user = currentUserService.getCurrentUser();
        UserList userList = userListRepository.save(new UserList(
                user,
                request.name().strip(),
                normalize(request.description()),
                Boolean.TRUE.equals(request.isPublic()),
                request.sortBy() == null ? ListSortBy.ORIGINAL_ORDER_ASC : request.sortBy()
        ));

        return UserListResponse.from(userList, 0, null);
    }

    @Transactional(readOnly = true)
    public UserListDetailsResponse getListDetails(Long listId, int page, int size, ListSortBy sortBy) {
        UserList userList = getOwnedList(listId);
        ListSortBy appliedSortBy = sortBy == null ? userList.getSortBy() : sortBy;

        Page<UserListItem> items = userListItemRepository.findByUserList_Id(
                listId,
                PageRequest.of(page, size, appliedSortBy.toSort())
        );

        return new UserListDetailsResponse(
                UserListResponse.from(userList, items.getTotalElements(), findLatestMediaItem(userList)),
                appliedSortBy,
                PageResponse.from(items.map(UserListItemResponse::from))
        );
    }

    @Transactional
    public UserListResponse updateList(Long listId, UpdateUserListRequest request) {
        UserList userList = getOwnedList(listId);
        MediaItem coverMediaItem = request.cover() == null ? null : findCoverMediaItem(listId, request.cover());

        userList.update(
                request.name().strip(),
                normalize(request.description()),
                request.isPublic(),
                request.sortBy(),
                coverMediaItem
        );
        userListRepository.saveAndFlush(userList);

        return UserListResponse.from(
                userList,
                userListItemRepository.countByUserList_Id(listId),
                findLatestMediaItem(userList)
        );
    }

    @Transactional
    public void deleteList(Long listId) {
        UserList userList = getOwnedList(listId);

        userListItemRepository.deleteAllByListId(listId);
        userListRepository.delete(userList);
    }

    public UserList getOwnedList(Long listId) {
        Long userId = currentUserService.getCurrentUser().getId();

        return userListRepository.findByIdAndUser_Id(listId, userId)
                .orElseThrow(UserListNotFoundException::new);
    }

    private MediaItem findCoverMediaItem(Long listId, ListCoverRequest cover) {
        return userListItemRepository
                .findByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(listId, cover.mediaType(), cover.tmdbId())
                .map(UserListItem::getMediaItem)
                .orElseThrow(CoverItemNotInListException::new);
    }

    private MediaItem findLatestMediaItem(UserList userList) {
        if (userList.getCoverMediaItem() != null) {
            return null;
        }

        return userListItemRepository.findFirstByUserList_IdOrderByIdDesc(userList.getId())
                .map(UserListItem::getMediaItem)
                .orElse(null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
