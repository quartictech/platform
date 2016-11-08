package io.quartic.common.client;

import feign.*;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.jaxrs.JAXRSContract;
import feign.slf4j.Slf4jLogger;

import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;

public final class ClientBuilder {
    private ClientBuilder() {}

    public static <T> T build(Class<T> target, Class<?> owner, String url) {
        return build(target, userAgentFor(owner), url);
    }

    private static <T> T build(Class<T> target, String userAgent, String url) {
        return Feign.builder()
                .contract(new JAXRSContract())
                .encoder(new JacksonEncoder(OBJECT_MAPPER))
                .decoder(new JacksonDecoder(OBJECT_MAPPER))
                .retryer(new Retryer.Default(0, 0, 1))
                .requestInterceptor(template -> template.header(USER_AGENT, userAgent))
                .logger(new Slf4jLogger(ClientBuilder.class))
                .logLevel(Logger.Level.BASIC)
                .target(target, url);
    }

    public static String userAgentFor(Class<?> clazz) {
        final String version = clazz.getPackage().getImplementationVersion();
        return clazz.getSimpleName() + "/" + ((version == null) ? "unknown" : version);
    }

}
