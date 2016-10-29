package io.quartic.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.jaxrs.JAXRSContract;
import feign.slf4j.Slf4jLogger;

public final class ClientBuilder {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    private ClientBuilder() {}

    public static <T> T build(Class<T> clazz, String url) {
        return Feign.builder()
                .contract(new JAXRSContract())
                .encoder(new JacksonEncoder(OBJECT_MAPPER))
                .decoder(new JacksonDecoder(OBJECT_MAPPER))
                .logger(new Slf4jLogger(ClientBuilder.class))
                .logLevel(Logger.Level.BASIC)
                .target(clazz, url);
    }
}
