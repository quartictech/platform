package io.quartic.weyl.common.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

public final class ObjectMappers {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    private ObjectMappers() {}

    public static String encode(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not encode", e);
        }
    }

    public static <T> T decode(String src, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(src, type);
        } catch (IOException e) {
            throw new RuntimeException("Could not decode", e);
        }
    }
}
