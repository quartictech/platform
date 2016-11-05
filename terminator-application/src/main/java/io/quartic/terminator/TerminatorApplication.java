package io.quartic.terminator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.common.pingpong.PingPongResource;

import javax.websocket.server.ServerEndpointConfig;

import static io.quartic.terminator.CatalogueProxy.catalogueFromUrl;

public class TerminatorApplication extends Application<TerminatorConfiguration> {
    private final WebsocketBundle websocketBundle = new WebsocketBundle(new ServerEndpointConfig[0]);

    public static void main(String[] args) throws Exception {
        new TerminatorApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<TerminatorConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(websocketBundle);
    }

    @Override
    public void run(TerminatorConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/*");   // Note: *not* /api/*
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        final CatalogueProxy catalogue = CatalogueProxy.builder()
                .catalogue(catalogueFromUrl(configuration.getCatalogueUrl()))
                .build();
        final TerminatorResource terminator = new TerminatorResource(catalogue);
        websocketBundle.addEndpoint(createSocketServer(terminator, environment.getObjectMapper()));

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(terminator);

        catalogue.start();
    }

    private ServerEndpointConfig createSocketServer(TerminatorResource terminator, ObjectMapper objectMapper) {
        final SocketServer socketServer = new SocketServer(terminator.featureCollectionsWithDatasetIds(), objectMapper);
        return ServerEndpointConfig.Builder
                .create(SocketServer.class, "/ws")
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) socketServer;
                    }
                })
                .build();
    }
}
