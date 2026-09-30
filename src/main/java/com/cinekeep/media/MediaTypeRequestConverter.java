package com.cinekeep.media;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class MediaTypeRequestConverter implements Converter<String, MediaType> {
    @Override
    public MediaType convert(String source) {
        return MediaType.fromValue(source);
    }
}
