package io.quartic.common.client;

import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.jaxrs.JAXRSContract;
import feign.slf4j.Slf4jLogger;

import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;

public final class ClientBuilder {
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
