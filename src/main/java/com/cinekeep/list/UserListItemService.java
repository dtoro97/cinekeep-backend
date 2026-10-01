package com.cinekeep.list;

import com.cinekeep.media.MediaItem;
import com.cinekeep.media.MediaItemService;
import com.cinekeep.media.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserListItemService {
    private final UserListService userListService;
    private final MediaItemService mediaItemService;
    private final UserListItemRepository userListItemRepository;

    public UserListItemService(
            UserListService userListService,
            MediaItemService mediaItemService,
            UserListItemRepository userListItemRepository
    ) {
        this.userListService = userListService;
        this.mediaItemService = mediaItemService;
        this.userListItemRepository = userListItemRepository;
    }

    @Transactional
    public UserListItemResponse addItem(Long listId, ListItemRequest request) {
        UserList userList = userListService.getOwnedList(listId);

        if (userListItemRepository.existsByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
                listId,
                request.mediaType(),
                request.tmdbId()
        )) {
            throw new ListItemAlreadyExistsException();
        }

        MediaItem mediaItem = mediaItemService.findOrCreate(request.toMediaItemSnapshot());
        UserListItem userListItem = userListItemRepository.save(
                new UserListItem(userList, mediaItem, normalize(request.comment()))
        );

        return UserListItemResponse.from(userListItem);
    }

    @Transactional
    public UserListItemResponse updateItem(
            Long listId,
            MediaType mediaType,
            Integer tmdbId,
            UpdateListItemRequest request
    ) {
        userListService.getOwnedList(listId);
        UserListItem userListItem = findItem(listId, mediaType, tmdbId);

        userListItem.updateComment(normalize(request.comment()));

        return UserListItemResponse.from(userListItem);
    }

    @Transactional
    public void removeItem(Long listId, MediaType mediaType, Integer tmdbId) {
        UserList userList = userListService.getOwnedList(listId);
        UserListItem userListItem = findItem(listId, mediaType, tmdbId);

        if (userList.hasCover(userListItem.getMediaItem())) {
            userList.clearCover();
        }

        userListItemRepository.delete(userListItem);
    }

    @Transactional
    public void clearItems(Long listId) {
        UserList userList = userListService.getOwnedList(listId);

        userList.clearCover();
        userListItemRepository.deleteAllByListId(listId);
    }

    private UserListItem findItem(Long listId, MediaType mediaType, Integer tmdbId) {
        return userListItemRepository
                .findByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(listId, mediaType, tmdbId)
                .orElseThrow(ListItemNotFoundException::new);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
