package com.cinekeep.mediastate;

import com.cinekeep.media.MediaType;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaStateController {
    private final MediaStateService mediaStateService;

    public MediaStateController(MediaStateService mediaStateService) {
        this.mediaStateService = mediaStateService;
    }

    @GetMapping("/{mediaType}/{tmdbId}/state")
    public MediaStateResponse getMediaState(
            @PathVariable MediaType mediaType,
            @PathVariable @Positive Integer tmdbId
    ) {
        return mediaStateService.getMediaState(mediaType, tmdbId);
    }
}
