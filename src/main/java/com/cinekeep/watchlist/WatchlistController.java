package com.cinekeep.watchlist;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public PageResponse<WatchlistItemResponse> getWatchlist(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String sortDirection
    ) {
        return watchlistService.getWatchlist(mediaType, page, size, Sort.Direction.fromString(sortDirection));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistItemResponse addToWatchlist(@Valid @RequestBody WatchlistItemRequest request) {
        return watchlistService.addToWatchlist(request);
    }

    @DeleteMapping("/{mediaType}/{tmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWatchlist(@PathVariable MediaType mediaType, @PathVariable Integer tmdbId) {
        watchlistService.removeFromWatchlist(mediaType, tmdbId);
    }
}
