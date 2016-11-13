package io.quartic.terminator;

import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.common.client.WebsocketListener;
import io.quartic.common.pingpong.PingPongResource;

import javax.websocket.server.ServerEndpointConfig;

import static io.quartic.common.server.WebsocketServerUtils.createEndpointConfig;

public class TerminatorApplication extends ApplicationBase<TerminatorConfiguration> {
    private final WebsocketBundle websocketBundle = new WebsocketBundle(new ServerEndpointConfig[0]);

    public static void main(String[] args) throws Exception {
        new TerminatorApplication().run(args);
    }

    public TerminatorApplication() {
        super("terminator");
    }

    @Override
    public void initialize(Bootstrap<TerminatorConfiguration> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.addBundle(websocketBundle);
    }

    @Override
    public void run(TerminatorConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        final CatalogueWatcher catalogueWatcher = CatalogueWatcher.of(WebsocketListener.Factory.of(
                configuration.getCatalogueWatchUrl(),
                new WebsocketClientSessionFactory(getClass())
        ));
        final TerminatorResource terminator = new TerminatorResource(catalogueWatcher);
        websocketBundle.addEndpoint(
                createEndpointConfig("/ws", new SocketServer(terminator.featureCollections(), environment.getObjectMapper())));

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(terminator);

        catalogueWatcher.start();
    }

}
