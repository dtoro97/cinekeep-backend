package com.cinekeep.list;

import com.cinekeep.media.MediaType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists/{listId}/items")
public class UserListItemController {
    private final UserListItemService userListItemService;

    public UserListItemController(UserListItemService userListItemService) {
        this.userListItemService = userListItemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserListItemResponse addItem(@PathVariable Long listId, @Valid @RequestBody ListItemRequest request) {
        return userListItemService.addItem(listId, request);
    }

    @PutMapping("/{mediaType}/{tmdbId}")
    public UserListItemResponse updateItem(
            @PathVariable Long listId,
            @PathVariable MediaType mediaType,
            @PathVariable Integer tmdbId,
            @Valid @RequestBody UpdateListItemRequest request
    ) {
        return userListItemService.updateItem(listId, mediaType, tmdbId, request);
    }

    @DeleteMapping("/{mediaType}/{tmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @PathVariable Long listId,
            @PathVariable MediaType mediaType,
            @PathVariable Integer tmdbId
    ) {
        userListItemService.removeItem(listId, mediaType, tmdbId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearItems(@PathVariable Long listId) {
        userListItemService.clearItems(listId);
    }
}
