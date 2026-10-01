package com.cinekeep.rating;

import com.cinekeep.common.PageResponse;
import com.cinekeep.media.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public PageResponse<RatingResponse> getRatings(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String sortDirection
    ) {
        return ratingService.getRatings(mediaType, page, size, Sort.Direction.fromString(sortDirection));
    }

    @GetMapping("/{mediaType}/{tmdbId}")
    public RatingResponse getRating(@PathVariable MediaType mediaType, @PathVariable @Positive Integer tmdbId) {
        return ratingService.getRating(mediaType, tmdbId);
    }

    @PutMapping("/{mediaType}/{tmdbId}")
    public RatingResponse rate(
            @PathVariable MediaType mediaType,
            @PathVariable @Positive Integer tmdbId,
            @Valid @RequestBody RatingRequest request
    ) {
        return ratingService.rate(mediaType, tmdbId, request);
    }

    @DeleteMapping("/{mediaType}/{tmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@PathVariable MediaType mediaType, @PathVariable @Positive Integer tmdbId) {
        ratingService.deleteRating(mediaType, tmdbId);
    }
}
