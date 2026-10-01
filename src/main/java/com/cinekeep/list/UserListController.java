package com.cinekeep.list;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lists")
public class UserListController {
    private final UserListService userListService;

    public UserListController(UserListService userListService) {
        this.userListService = userListService;
    }

    @GetMapping
    public PageResponse<UserListResponse> getLists(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return userListService.getLists(page, size);
    }

    @GetMapping("/membership")
    public ListMembershipResponse getMembership(
            @RequestParam @NotNull MediaType mediaType,
            @RequestParam @NotNull @Positive Integer tmdbId
    ) {
        return userListService.getMembership(mediaType, tmdbId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserListResponse createList(@Valid @RequestBody CreateUserListRequest request) {
        return userListService.createList(request);
    }

    @GetMapping("/{listId}")
    public UserListDetailsResponse getListDetails(
            @PathVariable Long listId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) ListSortBy sortBy
    ) {
        return userListService.getListDetails(listId, page, size, sortBy);
    }

    @PutMapping("/{listId}")
    public UserListResponse updateList(
            @PathVariable Long listId,
            @Valid @RequestBody UpdateUserListRequest request
    ) {
        return userListService.updateList(listId, request);
    }

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteList(@PathVariable Long listId) {
        userListService.deleteList(listId);
    }
}
