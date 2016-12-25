package io.quartic.common.client;

import feign.Feign;
import feign.Logger;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.jaxrs.JAXRSContract;
import feign.slf4j.Slf4jLogger;

import static com.google.common.net.HttpHeaders.USER_AGENT;
import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;

public final class ClientBuilder {
    private ClientBuilder() {}

    public static <T> T build(Class<T> target, Class<?> owner, String url) {
        return build(target, Utils.userAgentFor(owner), url);
    }

    private static <T> T build(Class<T> target, String userAgent, String url) {
        return Feign.builder()
                .contract(new JAXRSContract())
                .encoder(new InputStreamEncoder(new JacksonEncoder(objectMapper())))
                .decoder(new JacksonDecoder(objectMapper()))
                .retryer(new Retryer.Default(0, 0, 1))
                .requestInterceptor(template -> template.header(USER_AGENT, userAgent))
                .logger(new Slf4jLogger(ClientBuilder.class))
                .logLevel(Logger.Level.BASIC)
                .target(target, url);
    }

}
