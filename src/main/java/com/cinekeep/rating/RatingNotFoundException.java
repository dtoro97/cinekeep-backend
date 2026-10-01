package com.cinekeep.rating;

import com.cinekeep.common.NotFoundException;

public class RatingNotFoundException extends NotFoundException {
    public RatingNotFoundException() {
        super("Rating not found");
    }
}
