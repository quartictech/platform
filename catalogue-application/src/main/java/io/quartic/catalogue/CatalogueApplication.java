package io.quartic.catalogue;

import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetIdImpl;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;

import javax.websocket.server.ServerEndpointConfig;
import java.time.Clock;

import static io.quartic.common.websocket.WebsocketUtilsKt.serverEndpointConfig;

public class CatalogueApplication extends ApplicationBase<CatalogueConfiguration> {
    private final WebsocketBundle websocketBundle = new WebsocketBundle(new ServerEndpointConfig[0]);
    private final UidGenerator<DatasetId> didGenerator = RandomUidGenerator.of(DatasetIdImpl::of);

    public static void main(String[] args) throws Exception {
        new CatalogueApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<CatalogueConfiguration> bootstrap) {
        bootstrap.addBundle(websocketBundle);
    }

    @Override
    public void runApplication(CatalogueConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        final CatalogueResource catalogue = new CatalogueResource(didGenerator, Clock.systemUTC(), environment.getObjectMapper());

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(catalogue);

        websocketBundle.addEndpoint(serverEndpointConfig("/api/datasets/watch", catalogue));
    }
}
