package com.cinekeep.rating;

import com.cinekeep.common.NotFoundException;

public class EpisodeRatingNotFoundException extends NotFoundException {
    public EpisodeRatingNotFoundException() {
        super("Episode rating not found");
    }
}
