package com.cinekeep.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaItemService {
    private final MediaItemRepository mediaItemRepository;

    public MediaItemService(MediaItemRepository mediaItemRepository) {
        this.mediaItemRepository = mediaItemRepository;
    }

    @Transactional
    public MediaItem findOrCreate(MediaItemSnapshot snapshot) {
        return mediaItemRepository.findByMediaTypeAndTmdbId(snapshot.mediaType(), snapshot.tmdbId())
                .map(mediaItem -> {
                    mediaItem.updateSnapshot(
                            snapshot.title(),
                            snapshot.posterPath(),
                            snapshot.backdropPath(),
                            snapshot.overview(),
                            snapshot.releaseDate(),
                            snapshot.voteAverage(),
                            snapshot.voteCount()
                    );
                    return mediaItem;
                })
                .orElseGet(() -> mediaItemRepository.save(new MediaItem(
                        snapshot.tmdbId(),
                        snapshot.mediaType(),
                        snapshot.title(),
                        snapshot.posterPath(),
                        snapshot.backdropPath(),
                        snapshot.overview(),
                        snapshot.releaseDate(),
                        snapshot.voteAverage(),
                        snapshot.voteCount()
                )));
    }
}
