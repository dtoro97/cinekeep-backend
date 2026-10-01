package com.cinekeep.favorite;

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
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public PageResponse<FavoriteItemResponse> getFavorites(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String sortDirection
    ) {
        return favoriteService.getFavorites(mediaType, page, size, Sort.Direction.fromString(sortDirection));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteItemResponse addToFavorites(@Valid @RequestBody FavoriteItemRequest request) {
        return favoriteService.addToFavorites(request);
    }

    @DeleteMapping("/{mediaType}/{tmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromFavorites(@PathVariable MediaType mediaType, @PathVariable Integer tmdbId) {
        favoriteService.removeFromFavorites(mediaType, tmdbId);
    }
}
