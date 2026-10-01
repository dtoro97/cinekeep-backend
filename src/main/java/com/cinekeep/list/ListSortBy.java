package com.cinekeep.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.domain.Sort;

public enum ListSortBy {
    ORIGINAL_ORDER_ASC("original_order.asc", "addedAt", Sort.Direction.ASC),
    ORIGINAL_ORDER_DESC("original_order.desc", "addedAt", Sort.Direction.DESC),
    TITLE_ASC("title.asc", "mediaItem.title", Sort.Direction.ASC),
    TITLE_DESC("title.desc", "mediaItem.title", Sort.Direction.DESC),
    RELEASE_DATE_ASC("release_date.asc", "mediaItem.releaseDate", Sort.Direction.ASC),
    RELEASE_DATE_DESC("release_date.desc", "mediaItem.releaseDate", Sort.Direction.DESC),
    VOTE_AVERAGE_ASC("vote_average.asc", "mediaItem.voteAverage", Sort.Direction.ASC),
    VOTE_AVERAGE_DESC("vote_average.desc", "mediaItem.voteAverage", Sort.Direction.DESC);

    private final String value;
    private final String property;
    private final Sort.Direction direction;

    ListSortBy(String value, String property, Sort.Direction direction) {
        this.value = value;
        this.property = property;
        this.direction = direction;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public Sort toSort() {
        return Sort.by(direction, property).and(Sort.by(direction, "id"));
    }

    @JsonCreator
    public static ListSortBy fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (ListSortBy sortBy : values()) {
            if (sortBy.value.equalsIgnoreCase(value)) {
                return sortBy;
            }
        }

        throw new IllegalArgumentException("Unsupported list sort: " + value);
    }
}
