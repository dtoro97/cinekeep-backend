package com.cinekeep.rating;

import com.cinekeep.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class EpisodeRatingController {
    private static final String EPISODE_PATH =
            "/tv/{seriesTmdbId}/seasons/{seasonNumber}/episodes/{episodeNumber}";

    private final EpisodeRatingService episodeRatingService;

    public EpisodeRatingController(EpisodeRatingService episodeRatingService) {
        this.episodeRatingService = episodeRatingService;
    }

    @GetMapping("/episodes")
    public PageResponse<EpisodeRatingResponse> getEpisodeRatings(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String sortDirection
    ) {
        return episodeRatingService.getEpisodeRatings(page, size, Sort.Direction.fromString(sortDirection));
    }

    @GetMapping(EPISODE_PATH)
    public EpisodeRatingResponse getEpisodeRating(
            @PathVariable @Positive Integer seriesTmdbId,
            @PathVariable @PositiveOrZero Integer seasonNumber,
            @PathVariable @Positive Integer episodeNumber
    ) {
        return episodeRatingService.getEpisodeRating(seriesTmdbId, seasonNumber, episodeNumber);
    }

    @PutMapping(EPISODE_PATH)
    public EpisodeRatingResponse rateEpisode(
            @PathVariable @Positive Integer seriesTmdbId,
            @PathVariable @PositiveOrZero Integer seasonNumber,
            @PathVariable @Positive Integer episodeNumber,
            @Valid @RequestBody EpisodeRatingRequest request
    ) {
        return episodeRatingService.rateEpisode(seriesTmdbId, seasonNumber, episodeNumber, request);
    }

    @DeleteMapping(EPISODE_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEpisodeRating(
            @PathVariable @Positive Integer seriesTmdbId,
            @PathVariable @PositiveOrZero Integer seasonNumber,
            @PathVariable @Positive Integer episodeNumber
    ) {
        episodeRatingService.deleteEpisodeRating(seriesTmdbId, seasonNumber, episodeNumber);
    }
}
