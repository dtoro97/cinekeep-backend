package com.cinekeep.list;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ListSortByRequestConverter implements Converter<String, ListSortBy> {
    @Override
    public ListSortBy convert(String source) {
        return ListSortBy.fromValue(source);
    }
}
